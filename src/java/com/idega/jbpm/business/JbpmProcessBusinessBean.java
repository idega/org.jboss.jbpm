package com.idega.jbpm.business;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.jbpm.def.DeployProcess;
import com.idega.jbpm.def.ViewToTask;

public class JbpmProcessBusinessBean {
	
	private JbpmContext getJbpmContext() {
		JbpmConfiguration config = getJbpmConfiguration();
		return config.createJbpmContext();
	}
	
	public List<ProcessDefinition> getProcessList() {
		JbpmContext ctx = getJbpmContext();
		try {
			return ctx.getGraphSession().findAllProcessDefinitions();
		} finally {
			if(ctx != null) {
				ctx.close();
			}
		}
	}
	
	public List<ProcessInstance> getProcessInstances(ProcessDefinition pd) {
		if (pd == null) {
			return null;
		}
		return getProcessInstances(pd.getId());
	}
	
	@SuppressWarnings("unchecked")
	public List<ProcessInstance> getProcessInstances(long processDefinitionId) {
		List<ProcessInstance> instances = null;
		JbpmContext ctx = getJbpmContext();
		try {
			List items = ctx.getGraphSession().findProcessInstances(processDefinitionId);
			if (items == null) {
				return null;
			}
			
			instances = new ArrayList<ProcessInstance>();
			Object o = null;
			for (int i = 0; i < items.size(); i++) {
				o = items.get(i);
				if (o instanceof ProcessInstance) {
					instances.add((ProcessInstance) o);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (ctx != null) {
				ctx.close();
			}
		}
		return instances;
	}
	
	public ProcessDefinition getProcessDefinition(String processId, JbpmContext ctx) {
		long id = new Long(processId).longValue();
		return ctx.getGraphSession().getProcessDefinition(id);
	}
	
	public ProcessDefinition getProcessDefinition(String processId) {
		JbpmContext ctx = getJbpmContext();
		try {
			long id = new Long(processId).longValue();
			return ctx.getGraphSession().getProcessDefinition(id);
		} finally {
			if(ctx != null) {
				ctx.close();
			}
		}
	}
	
	public void deployProcessDefinition(InputStream is) {
		JbpmContext ctx = getJbpmContext();
		try {
			ctx.deployProcessDefinition(ProcessDefinition.parseXmlInputStream(is));
		} catch (Exception e) {
			Logger.getLogger(DeployProcess.class.getName()).log(Level.WARNING, "Exception while deploying process definition", e);
//			TODO: display err msg				
		} finally {
			if(ctx != null) {
				ctx.close();
			}
		}
	}
	
	public Task getProcessTask(String processId, String taskName) {
		JbpmContext ctx = getJbpmContext();
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
			return mgmt.getTask(taskName);
		} finally {
			if(ctx != null) {
				ctx.close();
			}
		}
	}
	
	public List<AdvancedProperty> getProcessDefinitionTasks(String processId) {
		JbpmContext ctx = getJbpmContext();
		try {
			ProcessDefinition pd = getProcessDefinition(processId, ctx);
			TaskMgmtDefinition mgmt = pd.getTaskMgmtDefinition();
			List<AdvancedProperty> result = new ArrayList<AdvancedProperty>();
			for(Iterator it = mgmt.getTasks().keySet().iterator(); it.hasNext(); ) {
				String nextId = (String) it.next();
				Task task = mgmt.getTask(nextId);
				AdvancedProperty prop = new AdvancedProperty(task.getName(), task.getName());
				result.add(prop);
			}
			return result;
		} finally {
			if(ctx != null) {
				ctx.close();
			}
		}
	}
	
	private JbpmConfiguration cfg;
	private ViewToTask viewToTaskBinder;
	
	public ViewToTask getViewToTaskBinder() {
		return viewToTaskBinder;
	}

	public void setViewToTaskBinder(ViewToTask viewToTaskBinder) {
		this.viewToTaskBinder = viewToTaskBinder;
	}

	public void setJbpmConfiguration(JbpmConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public JbpmConfiguration getJbpmConfiguration() {
		return cfg;
	}

}
