package com.idega.jbpm.business;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

import com.idega.builder.bean.AdvancedProperty;

public class JbpmProcessBusinessBean {
	
	private SessionFactory sessionFactory;
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public List<ProcessDefinition> getProcessList() {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			@SuppressWarnings("unchecked")
			List<ProcessDefinition> processes = ctx.getGraphSession().findLatestProcessDefinitions();
			
			return processes;
			
		} finally {
			
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<ProcessInstance> getProcessInstances(ProcessDefinition pd) {
		if (pd == null) {
			return null;
		}
		return getProcessInstances(pd.getId());
	}
	
	public ProcessDefinition getProcessDefinition(String processId, JbpmContext ctx) {
		if(processId == null) {
			return null;
		}
		long id = new Long(processId).longValue();
		return ctx.getGraphSession().getProcessDefinition(id);
	}
	
	public ProcessDefinition getProcessDefinition(String processId) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			long id = new Long(processId).longValue();
			return ctx.getGraphSession().getProcessDefinition(id);
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<ProcessInstance> getProcessInstances(long processDefinitionId) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			@SuppressWarnings("unchecked")
			List<ProcessInstance> items = ctx.getGraphSession().findProcessInstances(processDefinitionId);
			if (items == null)
				return null;
			
//			FIXME: why is this needed? why cannot we use items directly?
			List<ProcessInstance> instances = new ArrayList<ProcessInstance>();
			instances.addAll(items);
			
			return instances;
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public Task getProcessTask(String processId, String taskName) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			return pd.getTaskMgmtDefinition().getTask(taskName);
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<Task> getProcessDefinitionTasks(ProcessDefinition pd) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		List<Task> result = new ArrayList<Task>();
		try {
//			ProcessDefinition pd = getProcessDefinition(processId, ctx);
//			if(pd != null) {
				TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
				for(Iterator it = mgmt.getTasks().keySet().iterator(); it.hasNext(); ) {
					String nextId = (String) it.next();
					Task task = mgmt.getTask(nextId);
					result.add(task);
//					if(forDWR) {
//						AdvancedProperty prop = new AdvancedProperty(task.getName(), task.getName());
//						result.add(prop);
//					} else {
//						SelectItem prop = new SelectItem(task.getName(), task.getName());
//						result.add(prop);
//					}
				}
//			}
			return result;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
//	FIXME: fix warnings - use generics
	public List getProcessDefinitionTasks(String processId, boolean forDWR) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		List result = new ArrayList();
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			if(pd != null) {
				TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
				for(Iterator it = mgmt.getTasks().keySet().iterator(); it.hasNext(); ) {
					String nextId = (String) it.next();
					Task task = mgmt.getTask(nextId);
					if(forDWR) {
						AdvancedProperty prop = new AdvancedProperty(task.getName(), task.getName());
						result.add(prop);
					} else {
						SelectItem prop = new SelectItem(task.getName(), task.getName());
						result.add(prop);
					}
				}
			}
			return result;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	private JbpmConfiguration cfg;
	
	public void setJbpmConfiguration(JbpmConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return cfg;
	}
}