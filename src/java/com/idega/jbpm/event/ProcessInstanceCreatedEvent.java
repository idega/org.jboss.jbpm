package com.idega.jbpm.event;

import java.io.Serializable;
import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class ProcessInstanceCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = 8587349554332579987L;

	private String processDefinitionName;
	private Serializable processDefintionId, processInstanceId;
	private Map<String, Object> variables;

	public ProcessInstanceCreatedEvent(String processDefinitionName, Long processInstanceId, Map<String, Object> variables) {
		super(processDefinitionName);

		this.processDefinitionName = processDefinitionName;
		this.processInstanceId = processInstanceId;
		this.variables = variables;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public <T extends Serializable> T getProcessDefintionId() {
		@SuppressWarnings("unchecked")
		T result = (T) processDefintionId;
		return result;
	}

	public <T extends Serializable> T getProcessInstanceId() {
		@SuppressWarnings("unchecked")
		T result = (T) processInstanceId;
		return result;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	@Override
	public String toString() {
		return "Notification about new process instance (ID: " + getProcessInstanceId() +
				") for proc. def. with name '" + getProcessDefinitionName() + "', variables: " + getVariables();
	}

}