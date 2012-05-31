package com.idega.jbpm.artifacts.presentation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.mail.MessagingException;

import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.block.email.presentation.EmailSender;
import com.idega.block.process.business.ProcessConstants;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogic;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.exe.BPMDocument;
import com.idega.jbpm.exe.BPMEmailDocument;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.jbpm.rights.Right;
import com.idega.jbpm.signing.SigningHandler;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWContext;
import com.idega.presentation.Image;
import com.idega.presentation.Layer;
import com.idega.presentation.PDFRenderedComponent;
import com.idega.presentation.Table2;
import com.idega.presentation.TableBodyRowGroup;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableHeaderCell;
import com.idega.presentation.TableHeaderRowGroup;
import com.idega.presentation.TableRow;
import com.idega.presentation.file.FileDownloadStatisticsViewer;
import com.idega.presentation.text.Heading3;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.CheckBox;
import com.idega.presentation.ui.GenericButton;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;

/**
 * TODO: access control checks shouldn't be done here at all - remake!
 * TODO: All this class is too big and total mess almost. Refactor 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.120 $ Last modified: $Date: 2009/07/16 14:03:38 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(ProcessArtifacts.SPRING_BEAN_NAME_PROCESS_ARTIFACTS)
public class ProcessArtifacts {
	
	private static final Logger LOGGER = Logger.getLogger(ProcessArtifacts.class.getName());
	public static final String SPRING_BEAN_NAME_PROCESS_ARTIFACTS = "BPMProcessAssets";
	
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private VariablesHandler variablesHandler;
	@Autowired
	private PermissionsFactory permissionsFactory;
	@Autowired
	private BuilderLogicWrapper builderLogicWrapper;
	@Autowired(required = false)
	private SigningHandler signingHandler;
	@Autowired(required = false)
	private VariableInstanceQuerier variablesQuerier;
	
	public static final String PROCESS_INSTANCE_ID_PARAMETER = "processInstanceIdParameter";
	public static final String TASK_INSTANCE_ID_PARAMETER = "taskInstanceIdParameter";
	
	private GridEntriesBean getDocumentsListDocument(IWContext iwc, Collection<BPMDocument> processDocuments, Long processInstanceId,
			ProcessArtifactsParamsBean params) {
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		int size = processDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		Locale userLocale = iwc.getCurrentLocale();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		String message = iwrb.getLocalizedString("generating", "Generating...");
		String pdfUri = bundle.getVirtualPathWithFileNameString("images/pdf.gif");
		String signPdfUri = bundle.getVirtualPathWithFileNameString("images/pdf_sign.jpeg");
		String errorMessage = iwrb.getLocalizedString("error_generating_pdf", "Sorry, unable to generate PDF file from selected document");
		
		GridEntriesBean entries = new GridEntriesBean(processInstanceId);
		for (BPMDocument submittedDocument : processDocuments) {
			Long taskInstanceId = submittedDocument.getTaskInstanceId();
			
			TaskInstanceW taskInstance = getBpmFactory().getProcessManagerByTaskInstanceId(taskInstanceId).getTaskInstance(taskInstanceId);
			ProcessInstanceW piw = taskInstance.getProcessInstanceW();
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			String rowId = taskInstanceId.toString();
			row.setId(rowId);
			
			boolean hasViewUI = submittedDocument.isHasViewUI();
			boolean renderableTask = isTaskRenderable(taskInstance);
			if (hasViewUI && !renderableTask) {
				row.setStyleClass("pdfViewableItem");
			}
			
			row.addCell(submittedDocument.getDocumentName());
			row.addCell(submittedDocument.getSubmittedByName());
			row.addCell(submittedDocument.getEndDate() == null ? CoreConstants.EMPTY
			                : new IWTimestamp(submittedDocument.getEndDate()).getLocaleDateAndTime(userLocale, IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);
			
			if (params.getDownloadDocument()) {
				row.addCell(renderableTask ?
						new StringBuilder("<img class=\"downloadCaseAsPdfStyle\" src=\"").append(pdfUri)
						.append("\" onclick=\"CasesBPMAssets.downloadCaseDocument(event, '").append(taskInstanceId).append("');\" />").toString() :
						"<span onclick=\"return false;\"></span>"
				);
			}
			
			if (params.getAllowPDFSigning()) {
				if (hasDocumentGeneratedPDF(taskInstanceId) || !submittedDocument.isSignable()) {
					// Sign icon will be in attachments' list (if not signed)
					row.addCell(CoreConstants.EMPTY);
				} else if (getSigningHandler() != null && !piw.hasEnded()) {
					row.addCell(new StringBuilder(
					                "<img class=\"signGeneratedFormToPdfStyle\" src=\"")
					                .append(signPdfUri)
					                .append("\" onclick=\"CasesBPMAssets.signCaseDocument")
					                .append(getJavaScriptActionForPDF(iwrb, taskInstanceId, null, message, errorMessage)).append("\" />")
					                .toString());
				}
			}
			
			// FIXME: don't use client side stuff for validating security constraints, check if
			// rights changer in this method.
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, taskInstanceId, null, null, true);
			}
			
			if (!hasViewUI) {
				entries.setRowHasViewUI(rowId, false);
			}
		}
		
		try {
			entries.setGridEntries(rows.getDocument());
			return entries;
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private boolean isTaskRenderable(TaskInstanceW taskInstance) {
		if (taskInstance == null) {
			return false;
		}
		
		UIComponent component = null;
		try {
			View view = taskInstance.getView();
			
			component = view.getViewForDisplay();
			if (component instanceof PDFRenderedComponent) {
				return !((PDFRenderedComponent) component).isPdfViewer();
			}
			
			return view.hasViewForDisplay();
		} catch(Exception e) {}
		
		return false;
	}
	
	private boolean hasDocumentGeneratedPDF(Long taskInstanceId) {
		
		try {
			List<BinaryVariable> binaryVariables = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId)
			        .getTaskInstance(taskInstanceId).getAttachments();
			
			if (ListUtil.isEmpty(binaryVariables)) {
				return false;
			}
			
			String expectedName = getFileNameForGeneratedPDFFromTaskInstance(taskInstanceId
			        .toString());
			for (BinaryVariable bv : binaryVariables) {
				if (expectedName.equals(bv.getFileName())) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public String getFileNameForGeneratedPDFFromTaskInstance(
	        String taskInstanceId) {
		return new StringBuilder("Document_").append(taskInstanceId).append(
		    ".pdf").toString();
	}
	
	private Document getEmailsListDocument(Collection<BPMEmailDocument> processEmails, Long processInstanceId, boolean rightsChanger, User currentUser) {
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		int size = processEmails.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		String userEmail = null;
		if (currentUser != null) {
			Email email = null;
			try {
				email = getUserBusiness().getUsersMainEmail(currentUser);
			} catch (Exception e) {}
			userEmail = email == null ? null : email.getEmailAddress();
		}

		String sendEmailComponent = getBuilderLogicWrapper().getBuilderService(iwc).getUriToObject(EmailSender.class, Arrays.asList(
			new AdvancedProperty(EmailSender.FROM_PARAMETER, userEmail),
			new AdvancedProperty(EmailSender.NAMES_FOR_EXTERNAL_PARAMETERS, PROCESS_INSTANCE_ID_PARAMETER),
			new AdvancedProperty(EmailSender.EXTERNAL_PARAMETERS, String.valueOf(processInstanceId)),
			new AdvancedProperty(EmailSender.ALLOW_CHANGE_RECIPIENT_ADDRESS_PARAMETER, Boolean.FALSE.toString())
		));
		String replyToSystemAddress = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_ACCOUNT);
		
		for (BPMEmailDocument email : processEmails) {
			
			String plainFrom = email.getFromAddress();
			String fromStr = plainFrom;
			
			if (email.getFromAddress() != null) {
				
				if (fromStr == null) {
					fromStr = email.getFromAddress();
				} else {
					fromStr = new StringBuilder(fromStr).append(" (").append(email.getFromAddress()).append(")").toString();
				}
			}
			
			Long taskInstanceId = email.getTaskInstanceId();
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			row.setId(taskInstanceId.toString());
			
			String subject = email.getSubject();
			row.addCell(subject);
			row.addCell(getEmailCell(sendEmailComponent, plainFrom, fromStr, subject, taskInstanceId, replyToSystemAddress));
			row.addCell(email.getEndDate() == null ? CoreConstants.EMPTY
			        : new IWTimestamp(email.getEndDate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);
			
			if (rightsChanger) {
				addRightsChangerCell(row, processInstanceId, taskInstanceId, null, null, true);
			}
		}
		
		try {
			return rows.getDocument();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private String getEmailCell(String componentUri, String emailAddress, String valueToShow, String subject, Long taskInstanceId, String replyTo) {
		if (StringUtil.isEmpty(emailAddress)) {
			return CoreConstants.EMPTY;
		}
		
		URIUtil uri = new URIUtil(componentUri);
		uri.setParameter(EmailSender.RECIPIENT_TO_PARAMETER, emailAddress);
		
		if (StringUtil.isEmpty(subject)) {
			subject = CoreConstants.EMPTY;
		}
		else {
			try {
				subject = URLEncoder.encode(subject, CoreConstants.ENCODING_UTF8);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		uri.setParameter(EmailSender.SUBJECT_PARAMETER, new StringBuilder("Re: ").append(subject).toString());
		
		String fullReplyTo = StringUtil.isEmpty(replyTo) ? emailAddress : new StringBuilder(emailAddress).append(CoreConstants.COMMA).append(replyTo).toString();
		uri.setParameter(EmailSender.REPLY_TO_PARAMETER, fullReplyTo);
		
		componentUri = new StringBuilder(uri.getUri()).append("&").append(EmailSender.EXTERNAL_PARAMETERS).append("=").append(String.valueOf(taskInstanceId))
		.append("&").append(EmailSender.NAMES_FOR_EXTERNAL_PARAMETERS).append("=").append(TASK_INSTANCE_ID_PARAMETER).toString();

		return new StringBuilder("<a class=\"iframe emailSenderLightboxinBPMCasesStyle\" href=\"").append(componentUri).append("\" ")
			.append("onclick=\"CasesBPMAssets.showSendEmailWindow(event);\">")
			.append(valueToShow).append("</a>").toString();
	}
	
	public GridEntriesBean getProcessDocumentsList(ProcessArtifactsParamsBean params) {
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null) {
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
			rows.setTotal(0);
			rows.setPage(0);
			
			try {
				GridEntriesBean entries = new GridEntriesBean();
				entries.setProcessInstanceId(processInstanceId);
				entries.setGridEntries(rows.getDocument());
				return entries;
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Exception while creating empty grid entries", e);
			}
		}
		
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		User loggedInUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getUserToUse();
		Locale userLocale = iwc.getCurrentLocale();
		
		ProcessInstanceW pi = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
		Collection<BPMDocument> processDocuments = pi.getSubmittedDocumentsForUser(loggedInUser, userLocale);
		
		return getDocumentsListDocument(iwc, processDocuments, processInstanceId, params);
	}
	
	public Document getProcessTasksList(ProcessArtifactsParamsBean params) {
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null)
			return null;
		
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		User loggedInUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getUserToUse();
		Locale userLocale = iwc.getCurrentLocale();
		
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		Collection<BPMDocument> tasksDocuments = null;
		try {
			tasksDocuments = loggedInUser == null ?
				null : 
				getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId).getTaskDocumentsForUser(loggedInUser, userLocale);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting tasks for process instance: " + processInstanceId + " and user: " + loggedInUser + " using locale: " +	userLocale, e);
		}
		tasksDocuments = tasksDocuments == null ? new ArrayList<BPMDocument>(0) : tasksDocuments;
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		int size = tasksDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		String noOneLocalized = iwrb.getLocalizedString("cases_bpm.case_assigned_to_no_one", "No one");
		String takeTaskImage = bundle.getVirtualPathWithFileNameString("images/take_task.png");
		String takeTaskTitle = iwrb.getLocalizedString("cases_bpm.case_take_task", "Take task");
		boolean allowReAssignTask = false;
		
		for (BPMDocument taskDocument: tasksDocuments) {
			boolean disableSelection = false; // this is not used now, and implementation can be different when we finally decide to use it
			
			Long taskInstanceId = taskDocument.getTaskInstanceId();
			
			final boolean addTaskAssigment;
			String assignedToName;
			
			if (StringUtil.isEmpty(taskDocument.getAssignedToName())) {
				addTaskAssigment = true; // Because is not assigned yet
				assignedToName = noOneLocalized;
			} else {
				addTaskAssigment = false;
				assignedToName = taskDocument.getAssignedToName();
			}
			
			if (addTaskAssigment || allowReAssignTask) {
				String imageId = new StringBuilder("id").append(taskInstanceId).append("_assignTask").toString();
				StringBuilder assignedToCell = new StringBuilder("<img src=\"").append(takeTaskImage).append("\" title=\"").append(takeTaskTitle).append("\"");
				assignedToCell.append(" id=\"").append(imageId).append("\"").append(" onclick=\"CasesBPMAssets.takeCurrentProcessTask(event, '")
					.append(taskInstanceId);
				assignedToCell.append("', '").append(imageId).append("', ").append(allowReAssignTask).append(");\" />");
				
				assignedToName = new StringBuilder(assignedToCell.toString()).append(CoreConstants.SPACE).append(assignedToName).toString();
			}
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			row.setOrder(taskDocument.getOrder());
			row.setId(taskInstanceId.toString());
			
			row.addCell(taskDocument.getDocumentName());
			row.addCell(taskDocument.getCreateDate() == null ? CoreConstants.EMPTY : new IWTimestamp(taskDocument.getCreateDate()).getLocaleDateAndTime(userLocale,
			                            IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);
			
			// TODO commented for future use. 'Taken by' column isn't shown now
			// row.addCell(assignedToName);
			
			disableSelection = true;
			
			if (disableSelection) {
				row.setStyleClass("disabledSelection");
				row.setDisabledSelection(disableSelection);
			}
			
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, taskInstanceId, null, null, true);
			}
		}
		
		if (ListUtil.isEmpty(rows.getRows())) {
			int cellsCount = 2;
			if (params.isRightsChanger()) {
				cellsCount++;
			}
			addMessageIfNoContentExists(rows, iwrb.getLocalizedString("no_tasks_available_currently", "You currently don't have any tasks awaiting"), cellsCount);
		}
		
		try {
			return rows.getDocument();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while parsing rows: " + rows, e);
			return null;
		}
	}
	
	private void addMessageIfNoContentExists(ProcessArtifactsListRows rows,
	        String message, int cellsCount) {
		rows.setTotal(1);
		rows.setPage(1);
		
		ProcessArtifactsListRow row = new ProcessArtifactsListRow();
		rows.addRow(row);
		
		row.setId("-1");
		
		row.addCell(message);
		cellsCount--;
		for (int i = 0; i < cellsCount; i++) {
			row.addCell(CoreConstants.EMPTY);
		}
		
		row.setStyleClass("disabledSelection");
		row.setDisabledSelection(true);
	}
	
	private void addRightsChangerCell(ProcessArtifactsListRow row,
	        Long processInstanceId, Long taskInstanceId,
	        Integer variableIdentifier, String userId,
	        boolean setSameRightsForAttachments) {
		
		final IWBundle bundle = IWMainApplication.getDefaultIWMainApplication()
		        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		final StringBuilder imageHtml = new StringBuilder(
		        "<img class=\"caseProcessResourceAccessRightsStyle\" src=\"");
		
		imageHtml
		        .append(
		            bundle
		                    .getVirtualPathWithFileNameString("images/preferences.png"))
		        .append("\" ondblclick=\"function() {}\"")
		        .append(
		            " onclick=\"CasesBPMAssets.showAccessRightsForBpmRelatedResourceChangeMenu(event, ")
		        .append(processInstanceId).append(", ").append(taskInstanceId)
		        .append(", this, ").append(variableIdentifier).append(", ")
		        .append(setSameRightsForAttachments).append(", ")
		        .append(userId).append(");\" />");
		
		row.addCell(imageHtml.toString());
	}
	
	public Document getTaskAttachments(ProcessArtifactsParamsBean params) {
		
		// TODO: check permission to view task variables
		
		Long taskInstanceId = params.getTaskId();
		
		if (taskInstanceId == null)
			return null;
		
		TaskInstanceW tiw = getBpmFactory().getProcessManagerByTaskInstanceId(
		    taskInstanceId).getTaskInstance(taskInstanceId);
		ProcessInstanceW piw = tiw.getProcessInstanceW();
		
		List<BinaryVariable> binaryVariables = tiw.getAttachments();
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		
		if (binaryVariables == null || binaryVariables.size() == 0) {
			return null; // This will result in 'closed' row in grid
		}
		
		int size = binaryVariables.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		
		String message = iwrb.getLocalizedString("signing", "Signing...");
		String image = bundle
		        .getVirtualPathWithFileNameString("images/pdf_sign.jpeg");
		String errorMessage = iwrb.getLocalizedString(
		    "unable_to_sign_attachment",
		    "Sorry, unable to sign selected attachment");
		
		String attachmentWindowLabel = null;
		String attachmentInfoImage = null;
		boolean canSeeStatistics = piw.hasRight(Right.processHandler);
		if (params.isShowAttachmentStatistics()) {
			params.setShowAttachmentStatistics(canSeeStatistics);	
		}
		if (params.isShowAttachmentStatistics()) {
			attachmentWindowLabel = iwrb.getLocalizedString("download_statistics", "Download statistics");
			attachmentInfoImage = bundle.getVirtualPathWithFileNameString("images/attachment_info.png");
		}
		
		for (BinaryVariable binaryVariable : binaryVariables) {
			
			if (binaryVariable.getHash() == null
			        || (binaryVariable.getHidden() != null && binaryVariable
			                .getHidden() == true))
				continue;
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			row.setId(binaryVariable.getHash().toString());
			
			String description = binaryVariable.getDescription();
			row.addCell(StringUtil.isEmpty(description) ? binaryVariable
			        .getFileName() : description);
			
			String fileName = binaryVariable.getFileName();
			row.addCell(new StringBuilder(
			        "<a href=\"javascript:void(0)\" rel=\"").append(fileName)
			        .append("\">").append(fileName).append("</a>").toString());
			
			Long fileSize = binaryVariable.getContentLength();
			row.addCell(FileUtil.getHumanReadableSize(fileSize == null ? Long
			        .valueOf(0) : fileSize));
			
			if (params.isShowAttachmentStatistics()) {
				row.addCell(new StringBuilder("<a class=\"BPMCaseAttachmentStatisticsInfo linkedWithLinker\" href=\"")
						.append(getAttachmentInfoWindowLink(iwc, binaryVariable, params.getCaseId(), taskInstanceId)).append("\" title=\"")
						.append(attachmentWindowLabel).append("\"><img src=\"").append(attachmentInfoImage).append("\"></img></a>").toString());
			}
			
			if (params.getAllowPDFSigning() && getSigningHandler() != null
			        && tiw.isSignable() && binaryVariable.isSignable()
			        && !tiw.getProcessInstanceW().hasEnded()) {
				if (isPDFFile(binaryVariable.getFileName())
				        && (binaryVariable.getSigned() == null || !binaryVariable
				                .getSigned())) {
					row
					        .addCell(new StringBuilder("<img src=\"")
					                .append(image)
					                .append(
					                    "\" onclick=\"CasesBPMAssets.signCaseAttachment")
					                .append(
					                    getJavaScriptActionForPDF(iwrb,
					                        taskInstanceId, binaryVariable
					                                .getHash().toString(),
					                        message, errorMessage)).append(
					                    "\" />").toString());
				} else {
					row.addCell(CoreConstants.EMPTY);
				}
			}
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, params.getPiId(), taskInstanceId,
				    binaryVariable.getHash(), null, false);
			}
		}
		
		try {
			if (!ListUtil.isEmpty(rows.getRows())) {
				
				return rows.getDocument();
			} else {
				return null;
			}
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	private String getAttachmentInfoWindowLink(IWApplicationContext iwac, BinaryVariable binaryVariable, String caseId, Long taskInstanceId) {
		String hash = String.valueOf(binaryVariable.getHash());
		
		//	TODO: remove hard-coding!
		String uri = BuilderLogic.getInstance().getUriToObject("is.idega.idegaweb.egov.bpm.artifacts.BPMFileDownloadsStatistics", Arrays.asList(
				new AdvancedProperty(FileDownloadStatisticsViewer.PARAMETER_FILE_HASH, hash),
				new AdvancedProperty(AttachmentWriter.PARAMETER_VARIABLE_HASH, hash),
				new AdvancedProperty(AttachmentWriter.PARAMETER_TASK_INSTANCE_ID, taskInstanceId.toString()),
				new AdvancedProperty("caseId", StringUtil.isEmpty(caseId) ? "-1" : caseId)
		));
		return uri;
	}
	
	private String getJavaScriptActionForPDF(IWResourceBundle iwrb,
	        Long taskInstanceId, String hashValue, String message,
	        String errorMessage) {
		hashValue = StringUtil.isEmpty(hashValue) ? CoreConstants.MINUS
		        : hashValue;
		
		return new StringBuilder("(event, '").append(taskInstanceId).append(
		    "', '").append(hashValue).append("','").append(message).append(
		    "', '").append(
		    iwrb.getLocalizedString("document_signing_form",
		        "Document signing form")).append("', '")
		        .append(
		            iwrb.getLocalizedString("close_signing_form",
		                "Close signing form")).append("', '").append(
		            errorMessage).append("');").toString();
	}
	
	private boolean isPDFFile(String fileName) {
		if (StringUtil.isEmpty(fileName)
		        || fileName.indexOf(CoreConstants.DOT) == -1) {
			return false;
		}
		
		String fileNameEnd = fileName.substring(fileName
		        .indexOf(CoreConstants.DOT));
		return StringUtil.isEmpty(fileNameEnd) ? false : fileNameEnd
		        .equalsIgnoreCase(".pdf");
	}
	
	public Document getEmailAttachments(ProcessArtifactsParamsBean params) {
		if (params == null) {
			return null;
		}
		
		return getTaskAttachments(params);
	}
	
	public Document getProcessEmailsList(ProcessArtifactsParamsBean params) {
		Long processInstanceId = params.getPiId();
		
		if (processInstanceId == null)
			return null;
		
		User loggedInUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getUserToUse();
		
		Collection<BPMEmailDocument> processEmails = null;
		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("load_bpm_emails", Boolean.TRUE))
			processEmails = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId).getAttachedEmails(loggedInUser);
		
		if (ListUtil.isEmpty(processEmails)) {
			try {
				ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
				rows.setTotal(0);
				rows.setPage(0);
				
				return rows.getDocument();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Exception while parsing rows", e);
				return null;
			}
		} else
			return getEmailsListDocument(processEmails, processInstanceId, params.isRightsChanger(), loggedInUser);
	}
	
	public Collection<User> getUsersConnectedToProces(ProcessInstanceW piw ){
		List<User> usersConnectedToProcess = piw.getUsersConnectedToProcess();
		if(ListUtil.isEmpty(usersConnectedToProcess)){
			return Collections.emptyList();
		}
		Set<User> users = new HashSet<User>(usersConnectedToProcess);
		return users;
	}
	
	@SuppressWarnings("unchecked")
	public Document getProcessContactsList(ProcessArtifactsParamsBean params) {
		if (params == null) {
			return null;
		}
		
		Long processInstanceId = params.getPiId();
		if (processInstanceId == null) {
			return null;
		}
		
		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId);
		
		Collection<User> uniqueUsers = Collections.emptyList();
		if (params.isShowOnlyCreatorInContacts()) {
			User owner = piw.getOwner();
			if (owner == null) {
				LOGGER.warning("Owner was not found for process instance: " + piw.getProcessInstanceId());
			} else {
				uniqueUsers = Arrays.asList(owner);
			}
		} else {
			uniqueUsers = getUsersConnectedToProces(piw);//piw.getUsersConnectedToProcess();
		}
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		rows.setTotal(uniqueUsers.size());
		rows.setPage(uniqueUsers.isEmpty() ? 0 : 1);
		
		String systemEmail = null;
		try {
			systemEmail = IWMainApplication.getDefaultIWApplicationContext()
			        .getApplicationSettings().getProperty(
			            CoreConstants.PROP_SYSTEM_ACCOUNT);
			
			if (systemEmail.indexOf("@") == -1) {
				String emailHost = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_HOST);
				systemEmail = systemEmail + "@" + emailHost.substring(emailHost.indexOf(".") + 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		String processIdentifier = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId).getProcessIdentifier();
		
		IWBundle bundle = IWMainApplication.getDefaultIWMainApplication()
		        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		for (User user : uniqueUsers) {
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			
			row.addCell(user.getName());
			row.addCell(getUserEmails(user.getEmails(), processIdentifier,
			    systemEmail));
			row.addCell(new StringBuilder(getUserPhones(user.getPhones()))
			        .append(getUserImage(bundle, user)).toString());
			
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, null, null, user
				        .getPrimaryKey().toString(), false);
			}
		}
		
		try {
			return rows.getDocument();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception parsing rows for contacts", e);
		}
		
		return null;
	}
	
	private String getUserImage(IWBundle bundle, User user) {
		String pictureUri = null;
		UserBusiness userBusiness = getUserBusiness();
		Image image = userBusiness.getUserImage(user);
		if (image == null) {
			// Default image
			boolean male = true;
			try {
				male = getUserBusiness().isMale(user.getGenderID());
			} catch (Exception e) {
				male = true;
			}
			pictureUri = new StringBuilder(bundle
			        .getVirtualPathWithFileNameString("images/")).append(
			    male ? "user_male" : "user_female").append(".png").toString();
		} else {
			pictureUri = image.getMediaURL(IWMainApplication
			        .getDefaultIWApplicationContext());
		}
		
		return new StringBuilder(
		        "<img class=\"userProfilePictureInCasesList\" src=\"").append(
		    pictureUri).append("\" />").toString();
	}
	
	private boolean canAddValueToCell(String value) {
		if (value == null) {
			return false;
		}
		
		if (CoreConstants.EMPTY.equals(value) || "null".equals(value)) {
			return false;
		}
		
		return true;
	}
	
	private String getUserPhones(Collection<Phone> phones) {
		if (phones == null || phones.isEmpty()) {
			return CoreConstants.MINUS;
		}
		
		int phonesCounter = 0;
		String phoneNumber = null;
		StringBuilder userPhones = new StringBuilder();
		boolean addSemicolon = false;
		for (Phone phone : phones) {
			phoneNumber = phone.getNumber();
			addSemicolon = false;
			
			if (!canAddValueToCell(phoneNumber)) {
				userPhones.append(CoreConstants.EMPTY);
			} else {
				addSemicolon = true;
				userPhones.append(phoneNumber);
			}
			if ((phonesCounter + 1) < phones.size() && addSemicolon) {
				userPhones.append(CoreConstants.SEMICOLON).append(
				    CoreConstants.SPACE);
			}
			
			phonesCounter++;
		}
		
		String result = userPhones.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS
		        : result;
	}
	
	private String getUserEmails(Collection<Email> emails,
	        String caseIdentifier, String systemEmail) {
		if (emails == null || emails.isEmpty()) {
			return CoreConstants.MINUS;
		}
		
		int emailsCounter = 0;
		String emailValue = null;
		StringBuilder userEmails = new StringBuilder();
		boolean addSemicolon = false;
		for (Email email : emails) {
			emailValue = email.getEmailAddress();
			addSemicolon = false;
			
			if (!canAddValueToCell(emailValue)) {
				userEmails.append(CoreConstants.EMPTY);
			} else {
				addSemicolon = true;
				userEmails.append(getContactEmailFormatted(emailValue,
				    caseIdentifier, systemEmail));
			}
			if ((emailsCounter + 1) < emails.size() && addSemicolon) {
				userEmails.append(CoreConstants.SPACE);
			}
			
			emailsCounter++;
		}
		
		String result = userEmails.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS
		        : result;
	}
	
	private String getContactEmailFormatted(String emailAddress,
	        String caseIdentifier, String systemEmail) {
		StringBuffer link = new StringBuffer("<a href=\"mailto:")
		        .append(emailAddress);
		
		boolean firstParamAdded = false;
		if (caseIdentifier != null) {
			link.append("?subject=(").append(caseIdentifier).append(")");
			firstParamAdded = true;
		}
		if (systemEmail != null) {
			if (firstParamAdded) {
				link.append("&");
			} else {
				link.append("?");
			}
			link.append("cc=").append(systemEmail);
		}
		
		link.append("\">").append(emailAddress).append("</a>");
		return link.toString();
	}
	
	protected String getTaskStatus(IWResourceBundle iwrb,
	        TaskInstance taskInstance) {
		
		if (taskInstance.hasEnded())
			return iwrb.getLocalizedString("ended", "Ended");
		if (taskInstance.getStart() != null)
			return iwrb.getLocalizedString("in_progess", "In progress");
		
		return iwrb.getLocalizedString("not_started", "Not started");
	}
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId) {
		try {
			IWContext iwc = getIWContext(false);
			if (iwc == null) {
				return null;
			}
			
			return getBuilderService().getRenderedComponent(iwc, getViewInUIComponent(taskInstanceId), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public UIComponent getViewInUIComponent(Long taskInstanceId)
	        throws Exception {
		return getViewInUIComponent(taskInstanceId, false);
	}
	
	public UIComponent getViewInUIComponent(Long taskInstanceId,
	        boolean pdfViewer) throws Exception {
		return getBpmFactory()
		        .getProcessManagerByTaskInstanceId(taskInstanceId)
		        .getTaskInstance(taskInstanceId).loadView().getViewForDisplay(
		            pdfViewer);
	}
	
	protected BuilderService getBuilderService() {
		
		try {
			return BuilderServiceFactory.getBuilderService(IWMainApplication
			        .getDefaultIWApplicationContext());
		} catch (RemoteException e) {
			throw new RuntimeException(
			        "Error while retrieving builder service", e);
		}
	}
	
	public boolean hasUserRolesEditorRights(Long processInstanceId) {
		
		if (processInstanceId == null)
			return false;
		
		try {
			Permission perm = getPermissionsFactory().getRightsMgmtPermission(
			    processInstanceId);
			getBpmFactory().getRolesManager().checkPermission(perm);
			
		} catch (AccessControlException e) {
			return false;
		}
		
		return true;
	}
	
	private IWContext getIWContext(boolean checkIfLogged) {
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			LOGGER.warning("IWContext is unavailable!");
		}
		return iwc;
	}
	
	public String setAccessRightsForProcessResource(String roleName, Long processInstanceId, Long taskInstanceId, String variableIdentifier,
			boolean hasReadAccess, boolean setSameRightsForAttachments, Integer userId) {
		
		String errorMessage = "Attachments permissions update failed!";
		
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return errorMessage;
		}
		
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		errorMessage = iwrb.getLocalizedString("attachments_permissions_update_failed", errorMessage);
		
		String errorToLog = "Got: roleName=" + roleName + ", taskInstanceId=" + taskInstanceId + ", userId=" + userId;
		if (StringUtil.isEmpty(roleName) || (taskInstanceId == null && userId == null)) {
			LOGGER.warning("Insufficient parameters provided. " + errorToLog);
			return errorMessage;
		}
		
		try {
			if (taskInstanceId == null) {
				ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
				piw.setContactsPermission(new Role(roleName, hasReadAccess ? Access.contactsCanBeSeen : null), userId);
			} else {
				TaskInstanceW tiw = getBpmFactory().getProcessManagerByTaskInstanceId(taskInstanceId).getTaskInstance(taskInstanceId);
				tiw.setTaskRolePermissions(new Role(roleName, hasReadAccess ? Access.read : null), setSameRightsForAttachments, variableIdentifier);				
			}
			
			return iwrb.getLocalizedString("attachments_permissions_successfully_updated", "Permissions successfully updated.");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error changing access rights. " + errorToLog, e);
		}
		
		return errorMessage;
	}
	
	public org.jdom.Document setRoleDefaultContactsForUser(
	        Long processInstanceId, Integer userId) {
		
		getBpmFactory().getRolesManager().setContactsPermission(
		    new Role("default"), processInstanceId, userId);
		
		return getContactsAccessRightsSetterBox(processInstanceId, userId);
	}
	
	public org.jdom.Document getAccessRightsSetterBox(Long processInstanceId,
	        Long taskInstanceId, String fileHashValue,
	        boolean setSameRightsForAttachments) {
		
		return getAccessRightsSetterBox(processInstanceId, taskInstanceId,
		    fileHashValue, setSameRightsForAttachments, null);
	}
	
	public org.jdom.Document getContactsAccessRightsSetterBox(
	        Long processInstanceId, Integer userId) {
		
		return getAccessRightsSetterBox(processInstanceId, null, null, null,
		    userId);
	}
	
	private org.jdom.Document getAccessRightsSetterBox(Long processInstanceId,
	        Long taskInstanceId, String fileHashValue,
	        Boolean setSameRightsForAttachments, Integer userId) {
		
		if (taskInstanceId == null && userId == null) {
			return null;
		}
		final IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		BuilderService builder = getBuilderService();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(
		    IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		if (builder == null) {
			return null;
		}
		Layer container = new Layer();
		
		final Collection<Role> roles;
		
		if (taskInstanceId != null) {
			
			// TODO: add method to taskInstanceW and get permissions from there
			TaskInstanceW tiw = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId)
			        .getTaskInstance(taskInstanceId);
			
			if (StringUtil.isEmpty(fileHashValue)) {
				
				roles = tiw.getRolesPermissions();
				
			} else {
				
				roles = tiw.getAttachmentRolesPermissions(fileHashValue);
			}
			
		} else {
			
			ProcessInstanceW piw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(processInstanceId)
			        .getProcessInstance(processInstanceId);
			
			roles = piw.getRolesContactsPermissions(userId);
		}
		
		List<String[]> accessParamsList = new ArrayList<String[]>();
		
		Layer buttonsContainer = new Layer();
		buttonsContainer.setStyleClass("links");
		Link closeLink = new Link(iwrb.getLocalizedString("close", "Close"));
		closeLink.setURL("javascript:void(0);");
		closeLink.setOnClick("CasesBPMAssets.closeAccessRightsSetterBox(event, '" + container.getId() + "');");
		
		if (ListUtil.isEmpty(roles)) {
			container.add(new Heading3(iwrb.getLocalizedString(
			    "no_roles_to_set_permissions",
			    "There are no roles to set access rights")));
			
			
			container.add(buttonsContainer);
			buttonsContainer.add(closeLink);
		} else {
			
			if (userId == null || setSameRightsForAttachments == null)
				setSameRightsForAttachments = false;
			
			container.add(new Heading3(iwrb.getLocalizedString(
			    "set_access_rights", "Set access rights")));
			
			Layer checkBoxes = new Layer();
			container.add(checkBoxes);
			Table2 table = new Table2();
			checkBoxes.add(table);
			TableHeaderRowGroup headerRowGroup = table.createHeaderRowGroup();
			TableRow headerRow = headerRowGroup.createRow();
			// Role name
			TableHeaderCell headerCell = headerRow.createHeaderCell();
			headerCell.add(new Text(iwrb.getLocalizedString("role_name",
			    "Role name")));
			// Permission to read
			headerCell = headerRow.createHeaderCell();
			
			if (taskInstanceId != null)
				headerCell.add(new Text(iwrb.getLocalizedString(
				    "allow_disallow_to_read", "Allow/disallow to read")));
			else
				headerCell.add(new Text(iwrb.getLocalizedString(
				    "allow_disallow_to_see_role_contacts",
				    "Allow/disallow to see contacts of role")));
			
			// Set same rights for attachments
			if (setSameRightsForAttachments) {
				headerCell = headerRow.createHeaderCell();
				headerCell.add(new Text(iwrb.getLocalizedString(
				    "set_same_permission_for_attachements",
				    "Set same access rights to attachments")));
			}
			
			TableBodyRowGroup bodyRowGroup = table.createBodyRowGroup();
			
			for (Role role : roles) {
				String roleName = role.getRoleName();
				
				TableRow bodyRow = bodyRowGroup.createRow();
				TableCell2 cell = bodyRow.createCell();
				
				cell.add(new Text(iwc.getIWMainApplication()
				        .getLocalisedStringMessage(roleName, roleName, null,
				            iwc.getCurrentLocale())));
				
				GenericButton sameRigthsSetter = null;
				if (setSameRightsForAttachments) {
					sameRigthsSetter = new GenericButton();
					Image setRightImage = new Image(
					        bundle
					                .getVirtualPathWithFileNameString("images/same_rights_button.png"));
					setRightImage.setTitle(iwrb.getLocalizedString(
					    "set_same_access_to_attachments_for_this_role",
					    "Set same access to attachments for this role"));
					setRightImage.setStyleClass("setSameAccessRightsStyle");
					sameRigthsSetter.setButtonImage(setRightImage);
				}
				
				CheckBox box = new CheckBox(roleName);
				
				if (taskInstanceId != null)
					box.setChecked(role.getAccesses() != null
					        && role.getAccesses().contains(Access.read));
				else
					box.setChecked(role.getAccesses() != null
					        && role.getAccesses().contains(
					            Access.contactsCanBeSeen));
				
				StringBuilder action = new StringBuilder(
				        "CasesBPMAssets.setAccessRightsForBpmRelatedResource('")
				        .append(box.getId()).append("', ");
				action.append(processInstanceId);
				action.append(", ").append(taskInstanceId).append(", ").append(
				    userId).append(", ");
				
				if (fileHashValue == null) {
					action.append("null");
				} else {
					action.append("'").append(fileHashValue).append("'");
				}
				action.append(", ");
				
				StringBuilder actionForCheckbox = new StringBuilder(action);
				box.setOnClick(actionForCheckbox.append("null").append(");")
				        .toString());
				cell = bodyRow.createCell();
				cell.setStyleClass("alignCenterText");
				cell.add(box);
				
				if (setSameRightsForAttachments) {
					String[] accessRightsParams = { box.getId(),
					        processInstanceId.toString(),
					        taskInstanceId.toString(), fileHashValue,
					        sameRigthsSetter.getId() };
					
					accessParamsList.add(accessRightsParams);
					
					cell = bodyRow.createCell();
					StringBuilder actionForButton = new StringBuilder(action);
					sameRigthsSetter.setOnClick(actionForButton.append("'")
					        .append(sameRigthsSetter.getId()).append("'")
					        .append(");").toString());
					cell.setStyleClass("alignCenterText");
					cell.add(sameRigthsSetter);
				}
			}
			
			container.add(buttonsContainer);
			buttonsContainer.add(closeLink);
			
			if (taskInstanceId == null) {
				Link setDefaultsLink = new Link(iwrb.getLocalizedString("bpm_resetToDefault", "Reset to default"));
				setDefaultsLink.setURL("javascript:void(0);");
				StringBuffer onclick = new StringBuffer("CasesBPMAssets.setRoleDefaultContactsForUser(event, ");
				onclick.append(processInstanceId).append(", ").append(userId).append(");");
				setDefaultsLink.setOnClick(onclick.toString());
				setDefaultsLink.setStyleClass("setRoleDefaults");
				
				buttonsContainer.add(setDefaultsLink);
			}
			
			
			if (setSameRightsForAttachments) {
				
				Image saveRigtsImage = new Image(bundle.getVirtualPathWithFileNameString("images/save_rights_button.png"));
				saveRigtsImage.setTitle(iwrb.getLocalizedString("set_same_access_to_attachments_for_all_roles", "Set same access to attachments for all roles"));
				saveRigtsImage.setStyleClass("setSameAccessRightsStyle");
				Link saveAllRightsButton = new Link(saveRigtsImage);
				saveAllRightsButton.setURL("javascript:void(0);");
				
				StringBuilder paramsArray = new StringBuilder("[ ");
				
				for (String[] params : accessParamsList) {
					paramsArray.append(" [ ");
					paramsArray.append(params[0] == null ? "null ," : "'"
					        + params[0] + "', ");
					paramsArray.append(params[1] == null ? "null ," : "'"
					        + params[1] + "', ");
					paramsArray.append(params[2] == null ? "null ," : "'"
					        + params[2] + "', ");
					paramsArray.append(params[3] == null ? "null ," : "'"
					        + params[3] + "', ");
					paramsArray.append(params[4] == null ? "null" : "'"
					        + params[4] + "'");
					paramsArray.append("] , ");
				}
				paramsArray.append("]");
				
				saveAllRightsButton.setOnClick("for each (params in " + paramsArray.toString() +
						") {CasesBPMAssets.setAccessRightsForBpmRelatedResource(params[0] ,params[1] ,params[2] ,params[3] ,params[4]); }");
				
				buttonsContainer.add(saveAllRightsButton);
			}
		}
		
		Link disableThisAttachmentForAllRoles = new Link(iwrb.getLocalizedString("disable_attachment_for_everybody", "Hide from all users"));
		disableThisAttachmentForAllRoles.setURL("javascript:void(0);");
		buttonsContainer.add(disableThisAttachmentForAllRoles);
		disableThisAttachmentForAllRoles.setOnClick(new StringBuilder("CasesBPMAssets.disableAttachmentForAllRoles(event, ")
			.append(fileHashValue).append(", ").append(processInstanceId).append(", ").append(taskInstanceId == null ? "null" : taskInstanceId)
		.append(");").toString());
		
		return builder.getRenderedComponent(iwc, container, false);
	}
	
	public boolean disableAttachmentForAllRoles(Integer fileHash, Long processInstanceId, Long taskInstanceId) {
		return getBpmFactory().getRolesManager().disableAttachmentForAllRoles(fileHash, processInstanceId, taskInstanceId);
	}
	
	public String takeBPMProcessTask(Long taskInstanceId, boolean reAssign) {
		if (taskInstanceId == null) {
			return null;
		}
		
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		User currentUser = null;
		try {
			currentUser = iwc.getCurrentUser();
		} catch (NotLoggedOnException e) {
			e.printStackTrace();
		}
		if (currentUser == null) {
			return null;
		}
		
		try {
			ProcessManager processManager = getBpmFactory()
			        .getProcessManagerByTaskInstanceId(taskInstanceId);
			TaskInstanceW taskInstance = processManager
			        .getTaskInstance(taskInstanceId);
			
			User assignedTo = taskInstance.getAssignedTo();
			if (assignedTo != null && !reAssign) {
				return assignedTo.getName();
			} else {
				taskInstance.assign(currentUser);
			}
			
			return getAssignedToYouLocalizedString(iwc.getIWMainApplication()
			        .getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER)
			        .getResourceBundle(iwc));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String watchOrUnwatchBPMProcessTask(Long processInstanceId) {
		String errorMessage = "Sorry, error occurred - can not fulfill your action";
		
		IWContext iwc = getIWContext(false);
		if (iwc == null) {
			return errorMessage;
		}
		
		IWResourceBundle iwrb = null;
		try {
			iwrb = iwc.getIWMainApplication().getBundle(
			    IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Can not get IWResourceBundle", e);
		}
		if (iwrb == null) {
			return errorMessage;
		}
		
		errorMessage = iwrb.getLocalizedString(
		    "cases_bpm.can_not_fulfill_action", errorMessage);
		
		if (processInstanceId == null) {
			return errorMessage;
		}
		
		ProcessWatch pwatch = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId).getProcessWatcher();
		
		if (pwatch.isWatching(processInstanceId)) {
			// Remove
			if (pwatch.removeWatch(processInstanceId)) {
				return pwatch.getWatchCaseStatusLabel(false);
			}
			
			return errorMessage;
		}
		
		// Add
		if (pwatch.takeWatch(processInstanceId)) {
			return pwatch.getWatchCaseStatusLabel(true);
		}
		
		return errorMessage;
	}
	
	public boolean assignCase(String handlerIdStr, Long processInstanceId) {
		ProcessInstanceW piw = getBpmFactory()
		        .getProcessManagerByProcessInstanceId(processInstanceId)
		        .getProcessInstance(processInstanceId);
		
		if (piw.hasRight(Right.processHandler)) {
			Integer handlerId = StringUtil.isEmpty(handlerIdStr) ? null : Integer.valueOf(handlerIdStr);
			piw.assignHandler(handlerId);

			notifyHandlerAboutAssignment(handlerId, processInstanceId);
			return Boolean.TRUE;
		}
		
		LOGGER.warning("No rights to take case");
		return Boolean.FALSE;
	}
	
	private void notifyHandlerAboutAssignment(Integer handlerId, Long processInstanceId) {
		if (handlerId == null || processInstanceId == null) {
			LOGGER.warning("Unable to notify hanlder (ID: " + handlerId + ") about assigned case (process instance ID: " + processInstanceId + ")");
			return;
		}
		
		UserBusiness userBusiness = getUserBusiness();
		Email email = null;
		try {
			email = userBusiness.getUserMail(handlerId);
		} catch (RemoteException e) {
			LOGGER.log(Level.WARNING, "Unable to get email for user: " + handlerId, e);
		}
		if (email == null) {
			return;
		}
		final String emailAddress = email.getEmailAddress();
		if (StringUtil.isEmpty(emailAddress)) {
			return;
		}
		
		IWApplicationContext iwac = IWMainApplication.getDefaultIWApplicationContext();
		final String from = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, CoreConstants.EMAIL_DEFAULT_FROM);
		final String host = iwac.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, CoreConstants.EMAIL_DEFAULT_HOST);
		
		IWContext iwc = getIWContext(false);
		IWResourceBundle iwrb = iwc == null ? null : iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		String subject = "Case assigned";
		subject = iwrb == null ? subject : iwrb.getLocalizedString("case_assigned", subject);
		
		String text = "A case \"case_identifier\", with name \"case_subject\" was assigned to you. Link to the case: case_link";
		String link = "unknown";
		text = iwrb == null ? text : iwrb.getLocalizedString("assigned_case_text", text);
		try {
			Collection<VariableInstanceInfo> info =
				getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(
						Arrays.asList(ProcessConstants.CASE_IDENTIFIER),
						Arrays.asList(processInstanceId),
						false,
						false,
						false
			);
			String replace = ListUtil.isEmpty(info) ? iwrb == null ? "unknown" : iwrb.getLocalizedString("unknown", "unknown") :
													info.iterator().next().getValue().toString();
			text = StringHandler.replace(text, "case_identifier", replace);
			
			info = getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(
						Arrays.asList(ProcessConstants.CASE_DESCRIPTION),
						Arrays.asList(processInstanceId),
						false,
						false,
						false
			);
			replace = ListUtil.isEmpty(info) ? iwrb == null ? "unknown" : iwrb.getLocalizedString("unknown", "unknown") :
													info.iterator().next().getValue().toString();
			text = StringHandler.replace(text, "case_subject", replace);
			
			BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getBPMUser(handlerId);
			bpmUser.setProcessInstanceId(processInstanceId);
			link = bpmUser.getUrlToTheProcess();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error resolving assets for case assigned notification message", e);
		}
		
		if (text.indexOf("case_link") != -1) {
			text = StringHandler.replace(text, "case_link", link);
		}
		
		final String emailSubject = subject;
		final String emailText = text;
		Thread sender = new Thread(new Runnable() {
			public void run() {
				try {
					SendMail.send(from, emailAddress, null, null, null, host, emailSubject, emailText);
				} catch (MessagingException e) {
					LOGGER.log(Level.WARNING, "Error sending case assignment notification to " + emailAddress, e);
				}
			}
		});
		sender.start();
	}
	
	public List<AdvancedProperty> getAllHandlerUsers(Long processInstanceId) {
		if (processInstanceId == null) {
			return Collections.emptyList();
		}
			
		ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);
		if (!piw.hasRight(Right.processHandler) || !piw.hasHandlerAssignmentSupport()) {
			return Collections.emptyList();
		}
				
		RolesManager rolesManager = getBpmFactory().getRolesManager();		
		List<String> caseHandlersRolesNames = rolesManager.getRolesForAccess(processInstanceId, Access.caseHandler);
		Collection<User> users = rolesManager.getAllUsersForRoles(caseHandlersRolesNames, processInstanceId);
				
		Integer assignedCaseHandlerId = piw.getHandlerId();
		String assignedCaseHandlerIdStr = assignedCaseHandlerId == null ? null : String.valueOf(assignedCaseHandlerId);
				
		List<AdvancedProperty> allHandlers = new ArrayList<AdvancedProperty>(1);
		IWContext iwc = getIWContext(true);
		if (iwc == null) {
			return null;
		}
		
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		AdvancedProperty ap = new AdvancedProperty(CoreConstants.EMPTY, iwrb.getLocalizedString("bpm.selectCaseHandler", "Select handler"));
		allHandlers.add(ap);
				
		for (User user : users) {
			String pk = String.valueOf(user.getPrimaryKey());
			ap = new AdvancedProperty(pk, user.getName());
			if (pk.equals(assignedCaseHandlerIdStr)) {
				ap.setSelected(true);
			}
			
			allHandlers.add(ap);
		}
				
		return allHandlers;
	}
	
	private String getAssignedToYouLocalizedString(IWResourceBundle iwrb) {
		return iwrb.getLocalizedString("cases_bpm.case_assigned_to_you", "You");
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}
	
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil
			        .getIWContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}
	
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
	public BuilderLogicWrapper getBuilderLogicWrapper() {
		return builderLogicWrapper;
	}
	
	public void setBuilderLogicWrapper(BuilderLogicWrapper builderLogicWrapper) {
		this.builderLogicWrapper = builderLogicWrapper;
	}
	
	SigningHandler getSigningHandler() {
		return signingHandler;
	}
	
	void setSigningHandler(SigningHandler signingHandler) {
		this.signingHandler = signingHandler;
	}
	
	public String getSigningAction(String taskInstanceId, String hashValue) {
		return getSigningHandler().getSigningAction(
		    Long.valueOf(taskInstanceId), hashValue);
	}
	
	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	VariableInstanceQuerier getVariablesQuerier() {
		return variablesQuerier;
	}

	void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}
}