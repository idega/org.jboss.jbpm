package com.idega.jbpm.artifacts.presentation;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.NotLoggedOnException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.Phone;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.artifacts.ProcessArtifactsProvider;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ProcessWatch;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMAccessControlException;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.jbpm.signing.SigningHandler;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.presentation.IWContext;
import com.idega.presentation.Image;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableBodyRowGroup;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableHeaderCell;
import com.idega.presentation.TableHeaderRowGroup;
import com.idega.presentation.TableRow;
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
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.69 $
 * 
 *          Last modified: $Date: 2008/10/01 07:43:57 $ by $Author: juozas $
 */
@Scope("singleton")
@Service(CoreConstants.SPRING_BEAN_NAME_PROCESS_ARTIFACTS)
public class ProcessArtifacts {

	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private VariablesHandler variablesHandler;
	@Autowired
	private ProcessArtifactsProvider processArtifactsProvider;
	@Autowired
	private PermissionsFactory permissionsFactory;
	@Autowired
	private BuilderLogicWrapper builderLogicWrapper;
	@Autowired
	private SigningHandler signingHandler;

	private Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());

	private Document getDocumentsListDocument(Collection<TaskInstanceW> processDocuments, Long processInstanceId, ProcessArtifactsParamsBean params) {

		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		RolesManager rolesManager = getBpmFactory().getRolesManager();

		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);

		// String uriToPO = getUriToSignerObject();
		String message = iwrb.getLocalizedString("generating", "Generating...");
		String pdfUri = bundle.getVirtualPathWithFileNameString("images/pdf.gif");
		String signPdfUri = bundle.getVirtualPathWithFileNameString("images/pdf_sign.jpeg");
		String errorMessage = iwrb.getLocalizedString("error_generating_pdf", "Sorry, unable to generate PDF file from selected document");
		for (TaskInstanceW submittedDocument : processDocuments) {

			try {
				Permission permission = getPermissionsFactory().getTaskViewPermission(true, submittedDocument.getTaskInstance());
				rolesManager.checkPermission(permission);

			} catch (BPMAccessControlException e) {
				continue;
			}

			String actorId = submittedDocument.getTaskInstance().getActorId();
			String submittedByName;

			if (actorId != null) {

				try {
					User usr = getUserBusiness().getUser(Integer.parseInt(actorId));
					submittedByName = usr.getName();

				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving actor name for actorId: " + actorId, e);
					submittedByName = CoreConstants.EMPTY;
				}

			} else
				submittedByName = CoreConstants.EMPTY;

			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			Long taskInstanceId = submittedDocument.getTaskInstance().getId();
			String tidStr = String.valueOf(taskInstanceId);
			row.setId(tidStr);

			row.addCell(submittedDocument.getName(iwc.getCurrentLocale()));
			row.addCell(submittedByName);
			row.addCell(submittedDocument.getTaskInstance().getEnd() == null ? CoreConstants.EMPTY : new IWTimestamp(submittedDocument.getTaskInstance().getEnd()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);

			if (params.getDownloadDocument()) {
				row.addCell(new StringBuilder("<img class=\"downloadCaseAsPdfStyle\" src=\"").append(pdfUri).append("\" onclick=\"CasesBPMAssets.downloadCaseDocument(event, '").append(tidStr).append("');\" />").toString());
			}

			String uri = "";// getSigningHandler().getSigningAction(taskInstanceId
			// , null);

			if (params.getAllowPDFSigning()) {
				// row.addCell(new StringBuilder(
				// "<img class=\"signGeneratedFormToPdfStyle\" src=\""
				// ).append(signPdfUri)
				// .append("\" onclick=\"CasesBPMAssets.signCaseDocument").append
				// (getSignerAction(iwrb, tidStr, null, signPdfUri, uriToPO,
				// message, errorMessage)).append("\" />")
				// .toString());
				
				
				String x = new StringBuilder("<img class=\"signGeneratedFormToPdfStyle\" src=\"").append(signPdfUri).
				append("\" onclick=\"CasesBPMAssets.signCaseDocument(").append("event,")
				.append(tidStr).append(",'").append(iwrb.getLocalizedString("document_signing_form", "Document signing form"))
				.append("', '").append(iwrb.getLocalizedString("close_signing_form", "Close signing form")).append("','"+ message+ "');\" />").toString();
				System.out.println(x);
				row.addCell(x);
			}
			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, tidStr, null, null, true);
			}
		}

		try {
			return rows.getDocument();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}

	private Document getEmailsListDocument(Collection<TaskInstance> processEmails, Long processInstanceId, boolean rightsChanger) {

		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processEmails.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		RolesManager rolesManager = getBpmFactory().getRolesManager();

		VariablesHandler variablesHandler = getVariablesHandler();

		for (TaskInstance email : processEmails) {

			try {

				Permission permission = getPermissionsFactory().getTaskViewPermission(true, email);
				rolesManager.checkPermission(permission);

			} catch (BPMAccessControlException e) {
				continue;
			}

			Map<String, Object> vars = variablesHandler.populateVariables(email.getId());

			String subject = (String) vars.get("string_subject");
			String fromPersonal = (String) vars.get("string_fromPersonal");
			String fromAddress = (String) vars.get("string_fromAddress");

			String fromStr = fromPersonal;

			if (fromAddress != null) {

				if (fromStr == null) {
					fromStr = fromAddress;
				} else {
					fromStr = new StringBuilder(fromStr).append(" (").append(fromAddress).append(")").toString();
				}
			}

			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(email.getId());
			row.setId(tidStr);

			row.addCell(subject);
			row.addCell(fromStr == null ? CoreConstants.EMPTY : fromStr);
			row.addCell(email.getEnd() == null ? CoreConstants.EMPTY : new IWTimestamp(email.getEnd()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);

			if (rightsChanger) {
				addRightsChangerCell(row, processInstanceId, tidStr, null, null, true);
			}
		}

		try {
			return rows.getDocument();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}

	public Document getProcessDocumentsList(ProcessArtifactsParamsBean params) {
		Long processInstanceId = params.getPiId();

		if (processInstanceId == null) {
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
			rows.setTotal(0);
			rows.setPage(0);

			try {
				return rows.getDocument();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Collection<TaskInstanceW> processDocuments = getProcessArtifactsProvider().getSubmittedTaskInstances(processInstanceId);

		return getDocumentsListDocument(processDocuments, processInstanceId, params);
	}

	public Document getProcessTasksList(ProcessArtifactsParamsBean params) {

		Long processInstanceId = params.getPiId();

		if (processInstanceId == null)
			return null;

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);

		Collection<TaskInstanceW> tasks = getProcessArtifactsProvider().getAllUnfinishedTaskInstances(processInstanceId);

		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = tasks.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);

		RolesManager rolesManager = getBpmFactory().getRolesManager();
		BPMUser bpmUsr = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
		Integer loggedInUserIdInt = bpmUsr.getIdToUse();
		String loggedInUserId = loggedInUserIdInt == null ? null : String.valueOf(loggedInUserIdInt);
		String youLocalized = getAssignedToYouLocalizedString(iwrb);
		String noOneLocalized = iwrb.getLocalizedString("cases_bpm.case_assigned_to_no_one", "No one");
		String takeTaskImage = bundle.getVirtualPathWithFileNameString("images/take_task.png");
		String takeTaskTitle = iwrb.getLocalizedString("cases_bpm.case_take_task", "Take task");
		boolean allowReAssignTask = false;

		for (TaskInstanceW taskInstance : tasks) {

			// if(taskInstance.getTaskInstance().getToken().hasEnded())
			// continue;

			try {

				Permission permission = getPermissionsFactory().getTaskSubmitPermission(false, taskInstance.getTaskInstance());
				rolesManager.checkPermission(permission);

			} catch (BPMAccessControlException e) {
				continue;
			}

			boolean disableSelection = false;
			String assignedToName;

			String tidStr = String.valueOf(taskInstance.getTaskInstance().getId());

			boolean addTaskAssigment = false;
			if (taskInstance.getTaskInstance().getActorId() != null) {

				if (loggedInUserId != null && taskInstance.getTaskInstance().getActorId().equals(loggedInUserId)) {
					disableSelection = false;
					assignedToName = youLocalized;

				} else {
					disableSelection = true;

					try {
						assignedToName = getUserBusiness().getUser(Integer.parseInt(taskInstance.getTaskInstance().getActorId())).getName();
					} catch (Exception e) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving actor name for actorId: " + taskInstance.getTaskInstance().getActorId(), e);
						assignedToName = CoreConstants.EMPTY;
					}
				}

			} else {
				addTaskAssigment = true; // Because is not assigned yet
				assignedToName = noOneLocalized;
			}
			if (addTaskAssigment || allowReAssignTask) {
				String imageId = new StringBuilder("id").append(tidStr).append("_assignTask").toString();
				StringBuilder assignedToCell = new StringBuilder("<img src=\"").append(takeTaskImage).append("\" title=\"").append(takeTaskTitle).append("\"");
				assignedToCell.append(" id=\"").append(imageId).append("\"").append(" onclick=\"CasesBPMAssets.takeCurrentProcessTask(event, '").append(tidStr);
				assignedToCell.append("', '").append(imageId).append("', ").append(allowReAssignTask).append(");\" />");

				assignedToName = new StringBuilder(assignedToCell.toString()).append(CoreConstants.SPACE).append(assignedToName).toString();
			}

			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);

			row.setId(tidStr);

			row.addCell(taskInstance.getName(iwc.getCurrentLocale()));
			row.addCell(taskInstance.getTaskInstance().getCreate() == null ? CoreConstants.EMPTY : new IWTimestamp(taskInstance.getTaskInstance().getCreate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT));
			row.setDateCellIndex(row.getCells().size() - 1);

			row.addCell(assignedToName);

			if (disableSelection) {

				row.setStyleClass("disabledSelection");
				row.setDisabledSelection(disableSelection);
			}

			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, tidStr, null, null, true);
			}
		}

		if (ListUtil.isEmpty(rows.getRows())) {
			int cellsCount = 3;
			if (params.isRightsChanger()) {
				cellsCount++;
			}
			addMessageIfNoContentExists(rows, iwrb.getLocalizedString("no_tasks_available_currently", "You currently don't have any tasks awaiting"), cellsCount);
		}

		try {
			return rows.getDocument();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}

	private void addMessageIfNoContentExists(ProcessArtifactsListRows rows, String message, int cellsCount) {
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

	private void addRightsChangerCell(ProcessArtifactsListRow row, Long processInstanceId, String taskInstanceId, Integer variableIdentifier, String userId, boolean setSameRightsForAttachments) {

		final IWBundle bundle = IWMainApplication.getDefaultIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);

		final StringBuilder imageHtml = new StringBuilder("<img class=\"caseProcessResourceAccessRightsStyle\" src=\"");

		imageHtml.append(bundle.getVirtualPathWithFileNameString("images/preferences.png")).append("\" ondblclick=\"function() {}\"").append(" onclick=\"CasesBPMAssets.showAccessRightsForBpmRelatedResourceChangeMenu(event, ").append(processInstanceId).append(", ").append(taskInstanceId).append(", this, ").append(variableIdentifier).append(", ").append(setSameRightsForAttachments).append(", ").append(userId).append(");\" />");

		row.addCell(imageHtml.toString());
	}

	public Document getTaskAttachments(ProcessArtifactsParamsBean params) {

		// TODO: check permission to view task variables

		Long taskInstanceId = params.getTaskId();

		if (taskInstanceId == null)
			return null;

		JbpmContext jctx = getIdegaJbpmContext().createJbpmContext();
		try {

			TaskInstance taskInstance = jctx.getTaskInstance(taskInstanceId);
			List<BinaryVariable> binaryVariables = getProcessArtifactsProvider().getTaskAttachments(taskInstanceId);
			RolesManager rolesManager = getBpmFactory().getRolesManager();

			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

			int size = binaryVariables.size();
			if (size == 0) {
				return null; // This will result in 'closed' row in grid
			}
			rows.setTotal(size);
			rows.setPage(size == 0 ? 0 : 1);

			IWContext iwc = CoreUtil.getIWContext();
			IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
			IWResourceBundle iwrb = bundle.getResourceBundle(iwc);

			String variableHash = null;
			String tidStr = taskInstanceId.toString();
			// String signerUri = getUriToSignerObject();
			String message = iwrb.getLocalizedString("signing", "Signing...");
			String image = bundle.getVirtualPathWithFileNameString("images/pdf_sign.jpeg");
			String errorMessage = iwrb.getLocalizedString("unable_to_sign_attachment", "Sorry, unable to sign selected attachment");

			for (BinaryVariable binaryVariable : binaryVariables) {

				if (binaryVariable.getHash() == null)
					continue;

				variableHash = binaryVariable.getHash().toString();
				try {
					Permission permission = getPermissionsFactory().getTaskVariableViewPermission(true, taskInstance, variableHash);
					rolesManager.checkPermission(permission);

				} catch (BPMAccessControlException e) {
					continue;
				}

				ProcessArtifactsListRow row = new ProcessArtifactsListRow();
				rows.addRow(row);
				row.setId(variableHash);

				String description = binaryVariable.getDescription();
				row.addCell(StringUtil.isEmpty(description) ? binaryVariable.getFileName() : description);
				row.addCell(binaryVariable.getFileName());

				Long fileSize = binaryVariable.getContentLength();
				row.addCell(FileUtil.getHumanReadableSize(fileSize == null ? Long.valueOf(0) : fileSize));

				if (params.getAllowPDFSigning()) {
					if (isPDFFile(binaryVariable.getFileName()) && (binaryVariable.getSigned() == null || !binaryVariable.getSigned())) {

						String uri = "";// getSigningHandler().getSigningAction(
						// taskInstanceId, variableHash);

						System.out.println(new StringBuilder("<img src=\"").append(image).append("\" onclick=\"CasesBPMAssets.signCaseAttachment")
							.append(new StringBuilder("(event, '").append(uri).append("', '").append(message).append("', '")
								.append(iwrb.getLocalizedString("document_signing_form", "Document signing form")).append("', '")
								.append(iwrb.getLocalizedString("close_signing_form", "Close signing form")).append("', '")
								.append(errorMessage).append("');").toString()).append("\" />").toString());

						row.addCell(new StringBuilder("<img src=\"").append(image).append("\" onclick=\"CasesBPMAssets.signCaseAttachment")
							.append(new StringBuilder("(event, '").append(uri).append("', '").append(message).append("', '")
								.append(iwrb.getLocalizedString("document_signing_form", "Document signing form")).append("', '")
								.append(iwrb.getLocalizedString("close_signing_form", "Close signing form")).append("', '")
								.append(errorMessage).append("');").toString()).append("\" />").toString());
					} else {
						row.addCell(CoreConstants.EMPTY);
					}
				}
				if (params.isRightsChanger()) {
					addRightsChangerCell(row, params.getPiId(), tidStr, binaryVariable.getHash(), null, false);
				}
			}

			try {
				return rows.getDocument();

			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception while parsing rows", e);
				return null;
			}
		} finally {
			getIdegaJbpmContext().closeAndCommit(jctx);
		}
	}

	// private String getUriToSignerObject() {
	// return getBuilderLogicWrapper().getBuilderService(IWMainApplication.
	// getDefaultIWApplicationContext
	// ()).getUriToObject(AscertiaSigningForm.class, null);
	// }

	// private String getSignerAction(IWResourceBundle iwrb, String
	// taskInstanceId, String hashValue, String image, String uri, String
	// message,
	// String errorMessage) {
	// String parameters = new
	// StringBuilder("['").append(AscertiaConstants.PARAM_TASK_ID
	// ).append("', '").append(AscertiaConstants.PARAM_VARIABLE_HASH)
	// .append("']").toString();
	// hashValue = StringUtil.isEmpty(hashValue) ? CoreConstants.MINUS :
	// hashValue;
	// String values = new
	// StringBuilder("['").append(taskInstanceId).append("', '"
	// ).append(hashValue).append("']").toString();
	// return new
	// StringBuilder("(event, '").append(uri).append("', ").append(parameters
	// ).append(", ").append(values).append(", '").append(message)
	// .append("', '").append(iwrb.getLocalizedString("document_signing_form",
	// "Document signing form")).append("', '")
	// .append(iwrb.getLocalizedString("close_signing_form",
	// "Close signing form")).append("', '").append(errorMessage).append("');")
	// .toString();
	// }

	private boolean isPDFFile(String fileName) {
		if (StringUtil.isEmpty(fileName) || fileName.indexOf(CoreConstants.DOT) == -1) {
			return false;
		}

		String fileNameEnd = fileName.substring(fileName.indexOf(CoreConstants.DOT));
		return StringUtil.isEmpty(fileNameEnd) ? false : fileNameEnd.equalsIgnoreCase(".pdf");
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

		Collection<TaskInstance> processEmails = getProcessArtifactsProvider().getAttachedEmailsTaskInstances(processInstanceId);

		if (processEmails == null || processEmails.isEmpty()) {

			try {

				ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
				rows.setTotal(0);
				rows.setPage(0);

				return rows.getDocument();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception while parsing rows", e);
				return null;
			}

		} else
			return getEmailsListDocument(processEmails, processInstanceId, params.isRightsChanger());
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

		ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);

		Collection<User> peopleConnectedToProcess = piw.getUsersConnectedToProcess();
		List<User> uniqueUsers = new ArrayList<User>();
		if (peopleConnectedToProcess != null) {
			for (User user : peopleConnectedToProcess) {
				if (!uniqueUsers.contains(user)) {
					uniqueUsers.add(user);
				}
			}
		}

		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();
		rows.setTotal(uniqueUsers.size());
		rows.setPage(uniqueUsers.isEmpty() ? 0 : 1);

		String systemEmail = null;
		try {
			systemEmail = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_ACCOUNT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String caseIdentifier = getProcessArtifactsProvider().getCaseIdentifier(processInstanceId);
		for (User user : uniqueUsers) {
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);

			row.addCell(user.getName());
			row.addCell(getUserEmails(user.getEmails(), caseIdentifier, systemEmail));
			row.addCell(getUserPhones(user.getPhones()));
			// row.addCell(getUserAddress(user));

			if (params.isRightsChanger()) {
				addRightsChangerCell(row, processInstanceId, null, null, user.getPrimaryKey().toString(), false);
			}
		}

		try {
			return rows.getDocument();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception parsing rows for contacts", e);
		}

		return null;
	}

	/*
	 * private String getUserAddress(User user) { UserBusiness userBusiness = null; try { userBusiness = (UserBusiness) IBOLookup.getServiceInstance(IWMainApplication .getDefaultIWApplicationContext(), UserBusiness.class); } catch (IBOLookupException e) { logger.log(Level.SEVERE, "Can not get instance of " + UserBusiness.class.getSimpleName(), e); } if (userBusiness == null) { return CoreConstants.MINUS; }
	 * 
	 * Address mainAddress = null; try { mainAddress = userBusiness.getUsersMainAddress(Integer.valueOf(user.getId())); } catch (NumberFormatException e) { e.printStackTrace(); } catch (EJBException e) { e.printStackTrace(); } catch (RemoteException e) { e.printStackTrace(); } if (mainAddress == null) { return CoreConstants.MINUS; }
	 * 
	 * StringBuilder userAddress = new StringBuilder(); String streetAddress = mainAddress.getStreetAddress(); if (canAddValueToCell(streetAddress)) { userAddress .append(streetAddress).append(CoreConstants.COMMA).append(CoreConstants .SPACE); }
	 * 
	 * String postalAddress = mainAddress.getPostalAddress(); if (canAddValueToCell(postalAddress)) { postalAddress = StringHandler.replace(postalAddress, "null", CoreConstants.EMPTY); userAddress.append(postalAddress).append(CoreConstants.SPACE); }
	 * 
	 * Country country = mainAddress.getCountry(); if (country != null) { String countryName = country.getName(); if (canAddValueToCell(countryName)) { userAddress.append(countryName); } }
	 * 
	 * return userAddress.toString(); }
	 */

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
				userPhones.append(CoreConstants.SEMICOLON).append(CoreConstants.SPACE);
			}

			phonesCounter++;
		}

		String result = userPhones.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS : result;
	}

	private String getUserEmails(Collection<Email> emails, String caseIdentifier, String systemEmail) {
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
				userEmails.append(getContactEmailFormatted(emailValue, caseIdentifier, systemEmail));
			}
			if ((emailsCounter + 1) < emails.size() && addSemicolon) {
				userEmails.append(CoreConstants.SPACE);
			}

			emailsCounter++;
		}

		String result = userEmails.toString();
		return result.equals(CoreConstants.EMPTY) ? CoreConstants.MINUS : result;
	}

	private String getContactEmailFormatted(String emailAddress, String caseIdentifier, String systemEmail) {
		StringBuffer link = new StringBuffer("<a href=\"mailto:").append(emailAddress);

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

	protected String getTaskStatus(IWResourceBundle iwrb, TaskInstance taskInstance) {

		if (taskInstance.hasEnded())
			return iwrb.getLocalizedString("ended", "Ended");
		if (taskInstance.getStart() != null)
			return iwrb.getLocalizedString("in_progess", "In progress");

		return iwrb.getLocalizedString("not_started", "Not started");
	}

	public org.jdom.Document getViewDisplay(Long taskInstanceId) {
		try {
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), getViewInUIComponent(taskInstanceId), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public UIComponent getViewInUIComponent(Long taskInstanceId) throws Exception {
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

		try {
			long processDefinitionId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			return getBpmFactory().getProcessManager(processDefinitionId).getTaskInstance(taskInstanceId).loadView().getViewForDisplay();
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	protected Collection<TaskInstance> getSubmittedTaskInstances(Long processInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

		try {
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);

			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();

			for (Iterator<TaskInstance> iterator = taskInstances.iterator(); iterator.hasNext();) {
				TaskInstance taskInstance = iterator.next();

				if (!taskInstance.hasEnded())
					iterator.remove();
			}

			return taskInstances;

		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	protected BuilderService getBuilderService() {

		try {
			return BuilderServiceFactory.getBuilderService(IWMainApplication.getDefaultIWApplicationContext());
		} catch (RemoteException e) {
			throw new RuntimeException("Error while retrieving builder service", e);
		}
	}

	public boolean hasUserRolesEditorRights(Long processInstanceId) {

		if (processInstanceId == null)
			return false;

		try {
			Permission perm = getPermissionsFactory().getRightsMgmtPermission(processInstanceId);
			getBpmFactory().getRolesManager().checkPermission(perm);

		} catch (AccessControlException e) {
			// logger.log(Level.SEVERE,
			// "Current user does not have rights to change access rights to resources of process: "
			// + processInstanceId, e);
			return false;
		}

		return true;
	}

	public String setAccessRightsForProcessResource(String roleName, Long processInstanceId, Long taskInstanceId, String variableIdentifier, boolean hasReadAccess, boolean setSameRightsForAttachments, Integer userId) {

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		IWBundle bundle = IWMainApplication.getDefaultIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);

		if (roleName == null || CoreConstants.EMPTY.equals(roleName) || (taskInstanceId == null && userId == null)) {
			logger.log(Level.WARNING, "setAccessRightsForProcessResource called, but insufficient parameters provided. Got: roleName=" + roleName + ", taskInstanceId=" + taskInstanceId + ", userId=" + userId);
			return iwrb.getLocalizedString("attachments_permissions_update_failed", "Attachments permissions update failed!");
		}

		if (taskInstanceId != null) {

			TaskInstanceW tiw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getTaskInstance(taskInstanceId);

			tiw.setTaskRolePermissions(new Role(roleName, hasReadAccess ? Access.read : null), setSameRightsForAttachments, variableIdentifier);

		} else {

			ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);

			piw.setContactsPermission(new Role(roleName, hasReadAccess ? Access.contactsCanBeSeen : null), userId);
		}

		return iwrb.getLocalizedString("attachments_permissions_successfully_updated", "Permissions successfully updated.");
	}

	public org.jdom.Document setRoleDefaultContactsForUser(Long processInstanceId, Integer userId) {

		getBpmFactory().getRolesManager().setContactsPermission(new Role("default"), processInstanceId, userId);

		return getContactsAccessRightsSetterBox(processInstanceId, userId);
	}

	public org.jdom.Document getAccessRightsSetterBox(Long processInstanceId, Long taskInstanceId, String fileHashValue, boolean setSameRightsForAttachments) {

		return getAccessRightsSetterBox(processInstanceId, taskInstanceId, fileHashValue, setSameRightsForAttachments, null);
	}

	public org.jdom.Document getContactsAccessRightsSetterBox(Long processInstanceId, Integer userId) {

		return getAccessRightsSetterBox(processInstanceId, null, null, null, userId);
	}

	private org.jdom.Document getAccessRightsSetterBox(Long processInstanceId, Long taskInstanceId, String fileHashValue, Boolean setSameRightsForAttachments, Integer userId) {

		if (taskInstanceId == null && userId == null) {
			return null;
		}
		final IWContext iwc = IWContext.getCurrentInstance();
		if (iwc == null) {
			return null;
		}

		BuilderService builder = getBuilderService();
		IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		if (builder == null) {
			return null;
		}
		Layer container = new Layer();

		final Collection<Role> roles;

		if (taskInstanceId != null) {

			roles = getBpmFactory().getRolesManager().getRolesPermissionsForTaskInstance(processInstanceId, taskInstanceId, CoreConstants.EMPTY.equals(fileHashValue) ? null : fileHashValue);
		} else {

			// get from the user
			roles = getBpmFactory().getRolesManager().getUserPermissionsForRolesContacts(processInstanceId, userId);
		}

		ArrayList<String[]> accessParamsList = new ArrayList<String[]>();

		Link closeLink = new Link(iwrb.getLocalizedString("close", "Close"));
		closeLink.setURL("javascript:void(0);");
		closeLink.setOnClick("CasesBPMAssets.closeAccessRightsSetterBox(this);");

		if (roles == null || roles.isEmpty()) {
			container.add(new Heading3(iwrb.getLocalizedString("no_roles_to_set_permissions", "There are no roles to set access rights")));

			Layer buttonsContainer = new Layer();
			container.add(buttonsContainer);

			container.add(closeLink);
		} else {

			if (userId == null || setSameRightsForAttachments == null)
				setSameRightsForAttachments = false;

			container.add(new Heading3(iwrb.getLocalizedString("set_access_rights", "Set access rights")));

			Layer checkBoxes = new Layer();
			container.add(checkBoxes);
			Table2 table = new Table2();
			checkBoxes.add(table);
			TableHeaderRowGroup headerRowGroup = table.createHeaderRowGroup();
			TableRow headerRow = headerRowGroup.createRow();
			// Role name
			TableHeaderCell headerCell = headerRow.createHeaderCell();
			headerCell.add(new Text(iwrb.getLocalizedString("role_name", "Role name")));
			// Permission to read
			headerCell = headerRow.createHeaderCell();

			if (taskInstanceId != null)
				headerCell.add(new Text(iwrb.getLocalizedString("allow_disallow_to_read", "Allow/disallow to read")));
			else
				headerCell.add(new Text(iwrb.getLocalizedString("allow_disallow_to_see_role_contacts", "Allow/disallow to see contacts of role")));

			// Set same rights for attachments
			if (setSameRightsForAttachments) {
				headerCell = headerRow.createHeaderCell();
				headerCell.add(new Text(iwrb.getLocalizedString("set_same_permission_for_attachements", "Set same access rights to attachments")));
			}

			TableBodyRowGroup bodyRowGroup = table.createBodyRowGroup();

			for (Role role : roles) {
				String roleName = role.getRoleName();

				TableRow bodyRow = bodyRowGroup.createRow();
				TableCell2 cell = bodyRow.createCell();
				cell.add(new Text(iwrb.getLocalizedString(roleName, roleName)));
				GenericButton sameRigthsSetter = null;
				if (setSameRightsForAttachments) {
					sameRigthsSetter = new GenericButton();
					Image setRightImage = new Image(bundle.getVirtualPathWithFileNameString("images/same_rights_button.png"));
					setRightImage.setToolTip(iwrb.getLocalizedString("set_same_access_to_attachments_for_this_role", "Set same access to attachments for this role"));
					setRightImage.setStyleClass("setSameAccessRightsStyle");
					sameRigthsSetter.setButtonImage(setRightImage);
				}

				CheckBox box = new CheckBox(roleName);

				if (taskInstanceId != null)
					box.setChecked(role.getAccesses() != null && role.getAccesses().contains(Access.read));
				else
					box.setChecked(role.getAccesses() != null && role.getAccesses().contains(Access.contactsCanBeSeen));

				StringBuilder action = new StringBuilder("CasesBPMAssets.setAccessRightsForBpmRelatedResource('").append(box.getId()).append("', ");
				action.append(processInstanceId);
				action.append(", ").append(taskInstanceId).append(", ").append(userId).append(", ");

				if (fileHashValue == null) {
					action.append("null");
				} else {
					action.append("'").append(fileHashValue).append("'");
				}
				action.append(", ");

				StringBuilder actionForCheckbox = new StringBuilder(action);
				box.setOnClick(actionForCheckbox.append("null").append(");").toString());
				cell = bodyRow.createCell();
				cell.setStyleClass("alignCenterText");
				cell.add(box);

				if (setSameRightsForAttachments) {
					String[] accessRightsParams = { box.getId(), processInstanceId.toString(), taskInstanceId.toString(), fileHashValue, sameRigthsSetter.getId() };

					accessParamsList.add(accessRightsParams);

					cell = bodyRow.createCell();
					StringBuilder actionForButton = new StringBuilder(action);
					sameRigthsSetter.setOnClick(actionForButton.append("'").append(sameRigthsSetter.getId()).append("'").append(");").toString());
					cell.setStyleClass("alignCenterText");
					cell.add(sameRigthsSetter);
				}
			}

			TableRow bodyRow = bodyRowGroup.createRow();
			TableCell2 cell = bodyRow.createCell();

			cell.add(closeLink);

			if (taskInstanceId == null) {

				Link setDefaultsLink = new Link(iwrb.getLocalizedString("bpm_resetToDefault", "Reset to default"));
				setDefaultsLink.setURL("javascript:void(0);");

				StringBuffer onclick = new StringBuffer("CasesBPMAssets.setRoleDefaultContactsForUser(this, ");

				onclick.append(processInstanceId).append(", ").append(userId).append(");");

				setDefaultsLink.setOnClick(onclick.toString());
				setDefaultsLink.setStyleClass("setRoleDefaults");

				cell.add(setDefaultsLink);
			}

			if (setSameRightsForAttachments) {

				GenericButton saveAllRightsButton = new GenericButton();
				Image saveRigtsImage = new Image(bundle.getVirtualPathWithFileNameString("images/save_rights_button.png"));
				saveRigtsImage.setToolTip(iwrb.getLocalizedString("set_same_access_to_attachments_for_all_roles", "Set same access to attachments for all roles"));
				saveRigtsImage.setStyleClass("setSameAccessRightsStyle");
				saveAllRightsButton.setButtonImage(saveRigtsImage);

				StringBuilder paramsArray = new StringBuilder("[ ");

				for (String[] params : accessParamsList) {
					paramsArray.append(" [ ");
					paramsArray.append(params[0] == null ? "null ," : "'" + params[0] + "', ");
					paramsArray.append(params[1] == null ? "null ," : "'" + params[1] + "', ");
					paramsArray.append(params[2] == null ? "null ," : "'" + params[2] + "', ");
					paramsArray.append(params[3] == null ? "null ," : "'" + params[3] + "', ");
					paramsArray.append(params[4] == null ? "null" : "'" + params[4] + "'");
					paramsArray.append("] , ");
				}
				paramsArray.append("]");

				saveAllRightsButton.setOnClick("for each (params in " + paramsArray.toString() + ") {CasesBPMAssets.setAccessRightsForBpmRelatedResource(params[0] ,params[1] ,params[2] ,params[3] ,params[4]); }");

				cell = bodyRow.createCell();
				cell.empty();
				cell = bodyRow.createCell();
				cell.setStyleClass("alignCenterText");
				cell.add(saveAllRightsButton);
			}
		}

		return builder.getRenderedComponent(iwc, container, false);
	}

	public String takeBPMProcessTask(Long taskInstanceId, boolean reAssign) {
		if (taskInstanceId == null) {
			return null;
		}

		IWContext iwc = CoreUtil.getIWContext();
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
			ProcessManager processManager = getBpmFactory().getProcessManagerByTaskInstanceId(taskInstanceId);
			TaskInstanceW taskInstance = processManager.getTaskInstance(taskInstanceId);

			User assignedTo = taskInstance.getAssignedTo();
			if (assignedTo != null && !reAssign) {
				return assignedTo.getName();
			} else {
				taskInstance.assign(currentUser);
			}

			return getAssignedToYouLocalizedString(iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public String watchOrUnwatchBPMProcessTask(Long processInstanceId) {
		String errorMessage = "Sorry, error occurred - can not fulfill your action";

		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return errorMessage;
		}

		IWResourceBundle iwrb = null;
		try {
			iwrb = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER).getResourceBundle(iwc);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Can not get IWResourceBundle", e);
		}
		if (iwrb == null) {
			return errorMessage;
		}

		errorMessage = iwrb.getLocalizedString("cases_bpm.can_not_fulfill_action", errorMessage);

		if (processInstanceId == null) {
			return errorMessage;
		}

		ProcessWatch pwatch = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId).getProcessWatcher();

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

	public String assignCase(String handlerIdStr, Long processInstanceId) {

		if (hasCaseHandlerRights(processInstanceId)) {

			Integer handlerId = handlerIdStr == null || CoreConstants.EMPTY.equals(handlerIdStr) ? null : new Integer(handlerIdStr);

			getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId).assignHandler(handlerId);

			return "great success";
		}

		return "no rights to take case";
	}

	public List<AdvancedProperty> getAllHandlerUsers(Long processInstanceId) {

		if (processInstanceId != null) {

			ProcessInstanceW piw = getBpmFactory().getProcessManagerByProcessInstanceId(processInstanceId).getProcessInstance(processInstanceId);

			if (hasCaseHandlerRights(processInstanceId) && piw.hasHandlerAssignmentSupport()) {

				RolesManager rolesManager = getBpmFactory().getRolesManager();

				List<String> caseHandlersRolesNames = rolesManager.getRolesForAccess(processInstanceId, Access.caseHandler);
				Collection<User> users = rolesManager.getAllUsersForRoles(caseHandlersRolesNames, processInstanceId);

				Integer assignedCaseHandlerId = piw.getHandlerId();

				String assignedCaseHandlerIdStr = assignedCaseHandlerId == null ? null : String.valueOf(assignedCaseHandlerId);

				ArrayList<AdvancedProperty> allHandlers = new ArrayList<AdvancedProperty>(1);

				IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
				IWBundle bundle = iwc.getIWMainApplication().getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
				IWResourceBundle iwrb = bundle.getResourceBundle(iwc);

				AdvancedProperty ap = new AdvancedProperty(CoreConstants.EMPTY, iwrb.getLocalizedString("bpm.caseHandler", "Case handler"));
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
		}

		return null;
	}

	public boolean hasCaseHandlerRights(Long processInstanceId) {

		try {
			Permission perm = getPermissionsFactory().getAccessPermission(processInstanceId, Access.caseHandler);
			getBpmFactory().getRolesManager().checkPermission(perm);
			return true;

		} catch (AccessControlException e) {
			return false;
		}
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
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
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

	public ProcessArtifactsProvider getProcessArtifactsProvider() {
		return processArtifactsProvider;
	}

	public void setProcessArtifactsProvider(ProcessArtifactsProvider processArtifactsProvider) {
		this.processArtifactsProvider = processArtifactsProvider;
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
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
		return getSigningHandler().getSigningAction(Long.valueOf(taskInstanceId), hashValue);

	}
}