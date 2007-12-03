package com.idega.jbpm.exe;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.w3c.dom.Document;

import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewCreator;
import com.idega.jbpm.def.ViewFactory;
import com.idega.jbpm.presentation.beans.ProcessArtifactsParamsBean;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2007/12/03 09:47:59 $ by $Author: civilis $
 */
public class ProcessArtifacts {
	
	private SessionFactory sessionFactory;
	private JbpmConfiguration jbpmConfiguration;
	private ViewCreator viewCreator;
	
	Logger logger = Logger.getLogger(ProcessArtifacts.class.getName());

	public Document processArtifactsList(ProcessArtifactsParamsBean params) {
		
		Integer processInstanceId = params.getPiId();
		
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
	
	public Document processTasksList(ProcessArtifactsParamsBean params) {
		
		Integer processInstanceId = params.getPiId();
		
		if(processInstanceId == null)
			return null;
	
		SessionFactory sessionFactory = getSessionFactory();
		
		Transaction transaction = sessionFactory.getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		Session session = sessionFactory.getCurrentSession();
		
		ctx.setSession(session);
		
		try {
			IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
//			FacesContext context = FacesContext.getCurrentInstance();
//			int initiatorId = IWContext.getIWContext(context).getCurrentUserId();
			
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			//TODO:
//			each token -> get tasks processInstance.findAllTokens()
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tasks = processInstance.getTaskMgmtInstance().getUnfinishedTasks(processInstance.getRootToken());
			
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
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public org.jdom.Document getArtifactViewDisplay(Long taskInstanceId) {
		
		return getViewDisplay(taskInstanceId, false);
	}
	
	protected org.jdom.Document getViewDisplay(Long taskInstanceId, boolean editable) {
		
//		TODO: should know the view type from task instance id 
		SessionFactory sessionFactory = getSessionFactory();
		
		Transaction transaction = sessionFactory.getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		Session session = sessionFactory.getCurrentSession();
		
		ctx.setSession(session);
		
		try {
			ViewTaskBind viewTaskBind = ViewTaskBind.getViewTaskBind(session, ctx.getTaskInstance(taskInstanceId).getTask().getId(), "xforms");
			ViewFactory viewFactory = getViewCreator().getViewFactory(viewTaskBind.getViewType());
			View view = viewFactory.createView(viewTaskBind);
			
			UIComponent viewUIComponent = view.getViewForDisplay(taskInstanceId);
			
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), viewUIComponent, true);
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public org.jdom.Document getTaskViewDisplay(Long taskInstanceId) {
		
		return getViewDisplay(taskInstanceId, true);
	}
	
	public List<ProcessArtifact> getProcessInstanceArtifacts(Integer processInstanceId) {

		SessionFactory sessionFactory = getSessionFactory();
		
		Transaction transaction = sessionFactory.getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		Session session = sessionFactory.getCurrentSession();
		
		ctx.setSession(session);
		
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
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}

	public ViewCreator getViewCreator() {
		return viewCreator;
	}

	public void setViewCreator(ViewCreator viewCreator) {
		this.viewCreator = viewCreator;
	}
	
	protected BuilderService getBuilderService() {
		
		try {
			return BuilderServiceFactory.getBuilderService(IWMainApplication.getDefaultIWApplicationContext());
		} catch (RemoteException e) {
			throw new RuntimeException("Error while retrieving builder service", e);
		}
	}
}