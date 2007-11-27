package com.idega.jbpm.exe;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public class ProcessArtifacts {
	
	private SessionFactory sessionFactory;
	private JbpmConfiguration jbpmConfiguration;
	private ViewCreator viewCreator;

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
			
			if(processArtifact.getCheckoutLink() == null)
				row.addCell(processArtifact.getCheckoutLink());
			else
				row.addCell(
						new StringBuilder("<a href=\"")
						.append(processArtifact.getCheckoutLink())
						.append("\">")
						.append(processArtifact.getName())
						.append("</a>")
						.toString()
				);				
			
			row.addCell(processArtifact.getCreateDate() == null ? CoreConstants.EMPTY :
					new IWTimestamp(processArtifact.getCreateDate()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)
					);
		}
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public org.jdom.Document getViewDisplay(Long taskInstanceId, String viewType) {
		
		SessionFactory sessionFactory = getSessionFactory();
		
		Transaction transaction = sessionFactory.getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		Session session = sessionFactory.getCurrentSession();
		
		ctx.setSession(session);
		
		try {
			ViewTaskBind viewTaskBind = ViewTaskBind.getViewTaskBind(session, ctx.getTaskInstance(taskInstanceId).getTask().getId(), viewType);
			ViewFactory viewFactory = getViewCreator().getViewFactory(viewTaskBind.getViewType());
			View view = viewFactory.createView(viewTaskBind);
			
			UIComponent viewUIComponent = view.getViewForDisplay(taskInstanceId);
			return getBuilderService().getRenderedComponent(IWContext.getIWContext(FacesContext.getCurrentInstance()), viewUIComponent, false);
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
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