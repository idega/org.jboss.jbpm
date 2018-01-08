package com.idega.jbpm.event;

import java.io.Serializable;
import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class VariableCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = -5977065467292706593L;

	private String processDefinitionName;
	private Serializable processInstanceId, taskInstanceId;
	private Map<String, Object> variables;

	public VariableCreatedEvent(Object source, String processDefinitionName, Serializable processInstanceId, Serializable taskInstanceId, Map<String, Object> variables) {
		super(source);

		this.processDefinitionName = processDefinitionName;
		this.processInstanceId = processInstanceId;
		this.taskInstanceId = taskInstanceId;
		this.variables = variables;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	public <T> T getProcessInstanceId() {
		@SuppressWarnings("unchecked")
		T id = (T) processInstanceId;
		return id;
	}

	public <T> T getTaskInstanceId() {
		@SuppressWarnings("unchecked")
		T id = (T) taskInstanceId;
		return id;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	public String toString() {
		return "Variables: " + getVariables() + " for process instance: " + getProcessInstanceId() + ", definition: " + getProcessDefinitionName();
	}
}