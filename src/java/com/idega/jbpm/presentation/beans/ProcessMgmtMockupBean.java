package com.idega.jbpm.presentation.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/28 11:09:51 $ by $Author: civilis $
 */
public class ProcessMgmtMockupBean {
	
	private List<SelectItem> processes = new ArrayList<SelectItem>();
	private List<SelectItem> processInstances = new ArrayList<SelectItem>();
	private List<SelectItem> taskInstances = new ArrayList<SelectItem>();
	private String processId;
	private String processInstanceId;
	private String taskInstanceId;

	public String getProcessInstanceId() {
		return processInstanceId;
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
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		
		try {
			
			@SuppressWarnings("unchecked")
			List<ProcessDefinition> pdList = ctx.getGraphSession().findLatestProcessDefinitions();
			
			for (ProcessDefinition processDefinition : pdList)
				processes.add(new SelectItem(String.valueOf(processDefinition.getId()), processDefinition.getName()));
			
			return processes;
			
		} finally {
			ctx.close();
		}
	}
	
	public List<SelectItem> getProcessInstances() {
		
		processInstances.clear();
		processInstances.add(new SelectItem("", "You choose"));
		
		if(getProcessId() == null || getProcessId().equals(""))
			return processInstances;
		
		long pid = Long.parseLong(getProcessId());
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		
		try {
			@SuppressWarnings("unchecked")
			List<ProcessInstance> piList = ctx.getGraphSession().findProcessInstances(pid);
			
			for (ProcessInstance processInstance : piList)
				processInstances.add(new SelectItem(String.valueOf(processInstance.getId()), processInstance.getRootToken().getNode().getName()));
			
			return processInstances;
			
		} finally {
			ctx.close();
		}
	}
	
	public List<SelectItem> getTaskInstances() {
		
		taskInstances.clear();
		taskInstances.add(new SelectItem("", "You choose"));
		
		if(getProcessInstanceId() == null || getProcessInstanceId().equals(""))
			return taskInstances;
		
		long piid = Long.parseLong(getProcessInstanceId());
		System.out.println("getTaskInstances for: "+piid);
		
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		
		try {
			
			ProcessInstance pi = ctx.getProcessInstance(piid);
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getUnfinishedTasks(pi.getRootToken());
			
			for (TaskInstance taskInstance : tis)
				taskInstances.add(new SelectItem(String.valueOf(taskInstance.getId()), taskInstance.getName()));
			
			return taskInstances;
			
		} finally {
			ctx.close();
		}
	}

	private JbpmConfiguration jbpmConfiguration;

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
}