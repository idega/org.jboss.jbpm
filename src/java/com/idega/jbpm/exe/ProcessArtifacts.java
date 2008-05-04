package com.idega.jbpm.exe;

import java.rmi.RemoteException;
import java.security.Permission;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.impl.BinaryVariable;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;
import com.idega.jbpm.identity.permission.ViewTaskParametersPermission;
import com.idega.jbpm.presentation.beans.ProcessArtifactsParamsBean;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.25 $
 *
 * Last modified: $Date: 2008/05/04 18:12:26 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("BPMProcessAssets")
public class ProcessArtifacts {
	
	private BPMFactory bpmFactory;
	private IdegaJbpmContext idegaJbpmContext;
	private VariablesHandler variablesHandler;
	private ProcessArtifactsProvider processArtifactsProvider;
	
	private Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());
	
	protected Document getDocumentsListDocument(Collection<TaskInstance> processDocuments) {
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processDocuments.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		RolesManager rolesManager = getBpmFactory().getRolesManager();

		for (TaskInstance submittedDocument : processDocuments) {
			
			try {
				Permission permission = getTaskSubmitPermission(true, submittedDocument);
				rolesManager.checkPermission(permission);
				
			} catch (BPMAccessControlException e) {
				continue;
			}
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(submittedDocument.getId());
			row.setId(tidStr);
			//row.addCell(tidStr);
			row.addCell(submittedDocument.getName());
			row.addCell(submittedDocument.getEnd() == null ? CoreConstants.EMPTY :
				new IWTimestamp(submittedDocument.getEnd()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
			);
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
		
//		TODO: don't return null, but empty doc instead
		if(processInstanceId == null)
			return null;
		
		Collection<TaskInstance> processDocuments = getProcessArtifactsProvider().getSubmittedTaskInstances(processInstanceId);
		
		return getDocumentsListDocument(processDocuments);
	}
	
	protected Permission getTaskSubmitPermission(boolean authPooledActorsOnly, TaskInstance taskInstance) {
		
		SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		
		return permission;
	}
	
	protected Permission getTaskViewPermission(boolean authPooledActorsOnly, TaskInstance taskInstance) {
		
		ViewTaskParametersPermission permission = new ViewTaskParametersPermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		
		return permission;
	}
	
	public Document getProcessTasksList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
		if(processInstanceId == null)
			return null;
	
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			List<Token> tokens = processInstance.findAllTokens();
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tasks = processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken());
			
			for (Token token : tokens) {
				
				if(!token.equals(processInstance.getRootToken())) {
			
					@SuppressWarnings("unchecked")
					Collection<TaskInstance> tsks = processInstance.getTaskMgmtInstance().getUnfinishedTasks(token);
					tasks.addAll(tsks);
				}
			}
			
			ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

			int size = tasks.size();
			rows.setTotal(size);
			rows.setPage(size == 0 ? 0 : 1);
			
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			String loggedInUserId = String.valueOf(iwc.getCurrentUserId());
			
			for (TaskInstance taskInstance : tasks) {
				
				if(taskInstance.getToken().hasEnded())
					continue;
				
				try {
					
					Permission permission = getTaskSubmitPermission(true, taskInstance);
					rolesManager.checkPermission(permission);
					
				} catch (BPMAccessControlException e) {
					continue;
				}
				
				boolean disableSelection = false;
				String assignedToName;
				
				if(taskInstance.getActorId() != null) {
					
					if(taskInstance.getActorId().equals(loggedInUserId)) {
						disableSelection = false;
						assignedToName = "You";
						
					} else {
						disableSelection = true;
						
						try {
							assignedToName = getUserBusiness().getUser(Integer.parseInt(taskInstance.getActorId())).getName();
						} catch (Exception e) {
							Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving actor name for actorId: "+taskInstance.getActorId(), e);
							assignedToName = CoreConstants.EMPTY;
						}
					}
					
				} else {
					
					assignedToName = "No one";
				}
				
				String status = getTaskStatus(taskInstance);
				
				ProcessArtifactsListRow row = new ProcessArtifactsListRow();
				rows.addRow(row);
				
				String tidStr = String.valueOf(taskInstance.getId());
				row.setId(tidStr);
				//row.addCell(tidStr);
				row.addCell(taskInstance.getName());
				row.addCell(taskInstance.getCreate() == null ? CoreConstants.EMPTY :
							new IWTimestamp(taskInstance.getCreate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
				);
				row.addCell(assignedToName);
				row.addCell(status);

				if(disableSelection) {
					
					row.setStyleClass("disabledSelection");
					row.setDisabledSelection(disableSelection);
				}
			}
			
			try {
				return rows.getDocument();
				
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception while parsing rows", e);
				return null;
			}
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Document getTaskAttachments(ProcessArtifactsParamsBean params) {
		
//		TODO: check permission to view task variables
		
		Long taskInstanceId = params.getTaskId();
		
		if(taskInstanceId == null)
			return null;
		
		List<BinaryVariable> binaryVariables = getProcessArtifactsProvider().getTaskAttachments(taskInstanceId);
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = binaryVariables.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		
		for (BinaryVariable binaryVariable : binaryVariables) {
			
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			String tidStr = String.valueOf(binaryVariable.getHash());
			row.setId(tidStr);
			row.addCell(binaryVariable.getFileName());
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	public Document getProcessEmailsList(ProcessArtifactsParamsBean params) {
		
		Long processInstanceId = params.getPiId();
		
		if(processInstanceId == null)
			return null;
		
		Collection<TaskInstance> processEmails = getProcessArtifactsProvider().getAttachedEmailsTaskInstances(processInstanceId);
		
		if(processEmails == null || processEmails.isEmpty()) {
			
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
			return getDocumentsListDocument(processEmails);
	}
	
	protected String getTaskStatus(TaskInstance taskInstance) {
		
		if(taskInstance.hasEnded())
			return "Ended";
		if(taskInstance.getStart() != null)
			return "In progress";
		
		return "Not started";
	}
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			long processDefinitionId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			
			UIComponent viewUIComponent = getBpmFactory().getProcessManager(processDefinitionId).getTaskInstance(taskInstanceId).loadView().getViewForDisplay();
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), viewUIComponent, true);
			
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
			
			for (Iterator<TaskInstance> iterator  = taskInstances.iterator(); iterator.hasNext();) {
				TaskInstance taskInstance = iterator.next();
				
				if(!taskInstance.hasEnded())
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

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}

	@Autowired
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}

	public ProcessArtifactsProvider getProcessArtifactsProvider() {
		return processArtifactsProvider;
	}

	@Autowired
	public void setProcessArtifactsProvider(
			ProcessArtifactsProvider processArtifactsProvider) {
		this.processArtifactsProvider = processArtifactsProvider;
	}
}