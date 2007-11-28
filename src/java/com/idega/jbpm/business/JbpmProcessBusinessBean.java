package com.idega.jbpm.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.faces.model.SelectItem;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

import com.idega.builder.bean.AdvancedProperty;

public class JbpmProcessBusinessBean {
	
	public static final String BEAN_ID = "jbpmProcessBusiness";
	
	private SessionFactory sessionFactory;
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public void deleteProcessDefinition(Long processId) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ctx.getGraphSession().deleteProcessDefinition(processId);
		} finally {
			
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
		
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
	
	public Set<String> getProcessVariables(Long processId, boolean qualified) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();
			Set<String> variables = new HashSet<String>();
			for(Iterator<String> it = tasks.keySet().iterator(); it.hasNext(); ) {
				String taskName = (String) it.next();
				Task task = (Task) tasks.get(taskName);
				TaskController controller = task.getTaskController();
				if(controller == null) {
					task.setTaskController(new TaskController());
				}
				List<VariableAccess> list = controller.getVariableAccesses();
				for(Iterator<VariableAccess> it2 = list.iterator(); it2.hasNext(); ) {
					VariableAccess va = (VariableAccess) it2.next();
					String fullName = va.getVariableName();
					if(qualified) {
						variables.add(fullName);
					} else {
						StringTokenizer stk = new StringTokenizer(fullName, ":");
						String type = stk.nextToken();
						String name = stk.nextToken();
						variables.add(name);
					}
				}
			}
			return variables;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
		
	}
	
	public Map<String, Set<String>> getProcessVariables(Long processId) {
		
		if(processId == null)
			return null;
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();
			Map<String, Set<String>> variables = new HashMap<String, Set<String>>();
			for(Iterator<String> it = tasks.keySet().iterator(); it.hasNext(); ) {
				String taskName = (String) it.next();
				Task task = (Task) tasks.get(taskName);
				TaskController controller = task.getTaskController();
				if(controller == null) {
					task.setTaskController(new TaskController());
				}
				List<VariableAccess> list = controller.getVariableAccesses();
				for(Iterator<VariableAccess> it2 = list.iterator(); it2.hasNext(); ) {
					VariableAccess va = (VariableAccess) it2.next();
					String fullName = va.getVariableName();
					StringTokenizer stk = new StringTokenizer(fullName, ":");
					String type = stk.nextToken();
					String name = stk.nextToken();
					if(variables.containsKey(type)) {
						variables.get(type).add(name);
					} else {
						Set<String> newList = new HashSet<String>();
						newList.add(name);
						variables.put(type, newList);
					}
				}
			}
			return variables;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
		
	}
	
	public Set<String> getProcessVariablesByDatatype(Long processId, String datatype) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();
			Set<String> variables = new HashSet<String>();
			for(Iterator<String> it = tasks.keySet().iterator(); it.hasNext(); ) {
				String taskName = (String) it.next();
				Task task = (Task) tasks.get(taskName);
				TaskController controller = task.getTaskController();
				if(controller == null) {
					task.setTaskController(new TaskController());
				}
				List<VariableAccess> list = controller.getVariableAccesses();
				for(Iterator<VariableAccess> it2 = list.iterator(); it2.hasNext(); ) {
					VariableAccess va = (VariableAccess) it2.next();
					String fullName = va.getVariableName();
					StringTokenizer stk = new StringTokenizer(fullName, ":");
					String type = stk.nextToken();
					String name = stk.nextToken();
					if(type.equals(datatype)) {
						variables.add(name);
					}
				}
			}
			return variables;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
		
	}
	
	public ProcessDefinition getProcessDefinition(String processId, JbpmContext ctx) {
		if(processId == null) {
			return null;
		}
		long id = new Long(processId).longValue();
		return ctx.getGraphSession().getProcessDefinition(id);
	}
	
	public ProcessDefinition getProcessDefinition(Long processId, JbpmContext ctx) {
		if(processId == null || ctx == null) {
			return null;
		}
		return ctx.getGraphSession().getProcessDefinition(processId);
	}
	
	public ProcessDefinition getProcessDefinition(Long processId) {
		
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
	
	public List<ProcessInstance> getProcessInstances(Long processDefinitionId) {
		
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
	
	public void addTaskVariable(Long processId, String taskName, String datatype, String variableName) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Task task = pd.getTaskMgmtDefinition().getTask(taskName);
			if(task != null) {
				VariableAccess newVariable = new VariableAccess(datatype + ":" + variableName, "rw", "");
				TaskController controller = task.getTaskController();
				List list = new ArrayList();
				if(controller == null) {
					controller = new TaskController();
				} else {
					list = controller.getVariableAccesses();
				}
				list.add(newVariable);
				controller.setVariableAccesses(list);
				task.setTaskController(controller);
			}
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<String> getTaskTransitions(Long processId, String taskName) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Task task = pd.getTaskMgmtDefinition().getTask(taskName);
			List<Transition> list = task.getTaskNode().getLeavingTransitions();
			List<String> result = new ArrayList<String>();
			for(Iterator<Transition> it = list.iterator(); it.hasNext(); ) {
				Transition transition = it.next();
				result.add(transition.getName());
			}
			
			return result;
			
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public Map<String, List<String>> getTaskVariables(Long processId, String taskName) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Task task = pd.getTaskMgmtDefinition().getTask(taskName);
			List list = task.getTaskController().getVariableAccesses();
			Map<String, List<String>> variables = new HashMap<String, List<String>>();
			for(Iterator it = list.iterator(); it.hasNext(); ) {
				VariableAccess va = (VariableAccess) it.next();
				String fullName = va.getVariableName();
				StringTokenizer stk = new StringTokenizer(fullName, ":");
				String type = stk.nextToken();
				String name = stk.nextToken();
				if(variables.containsKey(type)) {
					variables.get(type).add(name);
				} else {
					List<String> newList = new ArrayList<String>();
					newList.add(name);
					variables.put(type, newList);
				}
			}
			return variables;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
		
	}
	
	public List<String> getTaskVariablesByDatatype(Long processId, String taskName, String datatype) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			Task task = pd.getTaskMgmtDefinition().getTask(taskName);
			List<String> variables = new ArrayList<String>();
			if(task != null) {
				TaskController controller = task.getTaskController();
				if(controller != null) {
					List list = controller.getVariableAccesses();
					for(Iterator it = list.iterator(); it.hasNext(); ) {
						VariableAccess va = (VariableAccess) it.next();
						String fullName = va.getVariableName();
						StringTokenizer stk = new StringTokenizer(fullName, ":");
						String type = stk.nextToken();
						if(type.equals(datatype)) {
							variables.add(stk.nextToken());
						}
					}
				}
			}
			return variables;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
		
	}
	
	public Task getProcessTask(Long processId, String taskName) {
		
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
			TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
			for(Iterator it = mgmt.getTasks().keySet().iterator(); it.hasNext(); ) {
				String nextId = (String) it.next();
				Task task = mgmt.getTask(nextId);
				result.add(task);
			}
			return result;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
	public List<Task> getProcessDefinitionTasks(Long processId) {
		
		Transaction transaction = getSessionFactory().getCurrentSession().getTransaction();
		boolean transactionWasActive = transaction.isActive();
		
		if(!transactionWasActive)
			transaction.begin();
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		ctx.setSession(getSessionFactory().getCurrentSession());
		
		List<Task> result = new ArrayList<Task>();
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			if(pd != null) {
				TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
				for(Iterator it = mgmt.getTasks().keySet().iterator(); it.hasNext(); ) {
					String nextId = (String) it.next();
					Task task = mgmt.getTask(nextId);
					result.add(task);
				}
			}
			return result;
		} finally {
			ctx.close();
			
			if(!transactionWasActive)
				transaction.commit();
		}
	}
	
//	FIXME: fix warnings - use generics
	public List getProcessDefinitionTasks(Long processId, boolean forDWR) {
		
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