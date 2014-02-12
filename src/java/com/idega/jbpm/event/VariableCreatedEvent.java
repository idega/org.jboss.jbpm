package com.idega.jbpm.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class VariableCreatedEvent extends ApplicationEvent {

	private static final long serialVersionUID = -5977065467292706593L;

	private String processDefinitionName;
	private Long processInstanceId, taskInstanceId;
	private Map<String, Object> variables;

	public VariableCreatedEvent(Object source, String processDefinitionName, Long processInstanceId, Long taskInstanceId, Map<String, Object> variables) {
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

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	public String toString() {
		return "Variables: " + getVariables() + " for process instance: " + getProcessInstanceId() + ", definition: " + getProcessDefinitionName();
	}
}