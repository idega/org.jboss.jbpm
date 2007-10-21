package com.idega.jbpm.presentation.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewToTask;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/10/21 21:19:20 $ by $Author: civilis $
 */
public class ProcessMgmtMockupBean {
	
	private List<SelectItem> processes = new ArrayList<SelectItem>();
	private List<SelectItem> processInstances = new ArrayList<SelectItem>();
	private List<SelectItem> taskInstances = new ArrayList<SelectItem>();
	private String processId;
	private String processInstanceId;
	private String taskInstanceId;
	private ViewToTask viewToTask;
	private String formId;
	private SessionFactory sessionFactory;

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setProcessInstanceId(String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public String getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(String taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}
	
	public List<SelectItem> getProcesses() {
		
		processes.clear();
		processes.add(new SelectItem("", "You choose"));
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			
			@SuppressWarnings("unchecked")
			List<ProcessDefinition> pdList = ctx.getGraphSession().findLatestProcessDefinitions();
			
			for (ProcessDefinition processDefinition : pdList)
				processes.add(new SelectItem(String.valueOf(processDefinition.getId()), processDefinition.getName()));
			
			return processes;
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<SelectItem> getProcessInstances() {
		
		processInstances.clear();
		processInstances.add(new SelectItem("", "You choose"));
		
		if(getProcessId() == null || getProcessId().equals(""))
			return processInstances;
		
		long pid = Long.parseLong(getProcessId());
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			@SuppressWarnings("unchecked")
			List<ProcessInstance> piList = ctx.getGraphSession().findProcessInstances(pid);
			
			for (ProcessInstance processInstance : piList)
				processInstances.add(new SelectItem(String.valueOf(processInstance.getId()), processInstance.getRootToken().getNode().getName() + (processInstance.hasEnded() ? " (Ended)" : processInstance.getStart() == null ? " (Not started)" : "")));
			
			return processInstances;
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<SelectItem> getTaskInstances() {
		
		taskInstances.clear();
		taskInstances.add(new SelectItem("", "You choose"));
		
		if(getProcessInstanceId() == null || getProcessInstanceId().equals(""))
			return taskInstances;
		
		long piid = Long.parseLong(getProcessInstanceId());
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			
			ProcessInstance pi = ctx.getProcessInstance(piid);
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
			
			for (TaskInstance taskInstance : tis)
				taskInstances.add(new SelectItem(String.valueOf(taskInstance.getId()), taskInstance.getName()));
			
			return taskInstances;
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}

	private JbpmConfiguration jbpmConfiguration;

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
	
	public void newProcessInstance() {
		
		if(getProcessId() == null || getProcessId().equals(""))
			return;
		
		long pid = Long.parseLong(getProcessId());
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			
			ProcessDefinition pd = ctx.getGraphSession().loadProcessDefinition(pid);
			ProcessInstance pi = pd.createProcessInstance();
			pi.setStart(new Date());
			pi.getRootToken().signal();
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public void takeTask() {
		
		if(getTaskInstanceId() == null || getTaskInstanceId().equals(""))
			return;
		
		long tid = Long.parseLong(getTaskInstanceId());
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			TaskInstance ti = ctx.getTaskInstance(tid);
			View view = getViewToTask().getView(ti.getTask().getId());
			
			if(view != null) {
				setFormId(view.getViewId());
			}
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}

	public ViewToTask getViewToTask() {
		return viewToTask;
	}

	public void setViewToTask(ViewToTask viewToTask) {
		this.viewToTask = viewToTask;
	}

	public String getFormId() {
		return formId;
	}

	public void setFormId(String formId) {
		
		this.formId = formId;
	}
	
	public void deleteProcessInstance() {
		System.out.println("delete pi not implemented");
		throw new RuntimeException("sorry for being here, but not available yet");
	}
	
	public void finishTaskInstance() {
		
		setFormId(null);
		
		if(getTaskInstanceId() == null || getTaskInstanceId().equals(""))
			return;
		
		long tid = Long.parseLong(getTaskInstanceId());
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			TaskInstance ti = ctx.getTaskInstance(tid);
			ti.end();
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
}