package com.idega.jbpm.exe;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.w3c.dom.Document;

import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.presentation.beans.ProcessArtifactsParamsBean;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $
 *
 * Last modified: $Date: 2008/03/11 12:16:59 $ by $Author: civilis $
 */
public class ProcessArtifacts {
	
	private BPMFactory bpmFactory;
	private IdegaJbpmContext idegaJbpmContext;
	
	private Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());

	public Document getProcessDocumentsList(ProcessArtifactsParamsBean params) {
		
		Integer processInstanceId = params.getPiId();
		
//		TODO: don't return null, but empty doc instead
		if(processInstanceId == null)
			return null;
		
		List<ProcessArtifact> processArtifacts = getProcessInstanceArtifacts(processInstanceId);
		
		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		int size = processArtifacts.size();
		rows.setTotal(size);
		rows.setPage(size == 0 ? 0 : 1);
		

		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());

//		artifacts columns: id, <link>label</link>, create date 
		for (ProcessArtifact processArtifact : processArtifacts) {
		
			ProcessArtifactsListRow row = new ProcessArtifactsListRow();
			rows.addRow(row);
			row.setId(processArtifact.getId());
			
			row.addCell(processArtifact.getId());
			//add link here
			
			row.addCell(processArtifact.getName());
			
			row.addCell(processArtifact.getCreateDate() == null ? CoreConstants.EMPTY :
					new IWTimestamp(processArtifact.getCreateDate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
					);
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception while parsing rows", e);
			return null;
		}
	}
	
	public Document getProcessTasksList(ProcessArtifactsParamsBean params) {
		
		Integer processInstanceId = params.getPiId();
		
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
			
			for (TaskInstance taskInstance : tasks) {
				
				ProcessArtifactsListRow row = new ProcessArtifactsListRow();
				rows.addRow(row);
				
				String tidStr = String.valueOf(taskInstance.getId());
				row.setId(tidStr);
				row.addCell(tidStr);
				row.addCell(taskInstance.getName());
				row.addCell(taskInstance.getCreate() == null ? CoreConstants.EMPTY :
							new IWTimestamp(taskInstance.getCreate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
				);
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
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
		
			long processDefinitionId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			
			UIComponent viewUIComponent = getBpmFactory().getViewManager(processDefinitionId).loadTaskInstanceView(taskInstanceId, FacesContext.getCurrentInstance()).getViewForDisplay();
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), viewUIComponent, true);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected List<ProcessArtifact> getProcessInstanceArtifacts(Integer processInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
			List<Long> finishedTaskIds = new ArrayList<Long>();
			
			for (TaskInstance taskInstance : taskInstances) {
				
				if(taskInstance.hasEnded())
					finishedTaskIds.add(taskInstance.getTask().getId());
			}
			
			List<ProcessArtifact> artifacts = new ArrayList<ProcessArtifact>();
			
			for (TaskInstance taskInstance : taskInstances) {
				
				if(taskInstance.hasEnded()) {
				
					ProcessArtifact artifact = new ProcessArtifact();
//					TODO: bind task instance with view type, so we know, what view type to display
					artifact.setId(String.valueOf(taskInstance.getId()));
					artifact.setName(taskInstance.getName());
					artifact.setCreateDate(taskInstance.getEnd());
					
					artifacts.add(artifact);
				}
			}
			
			return artifacts;
			
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

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}