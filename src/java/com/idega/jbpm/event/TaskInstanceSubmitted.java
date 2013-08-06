package com.idega.jbpm.event;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

import com.idega.jbpm.exe.TaskInstanceW;

public class TaskInstanceSubmitted extends ApplicationEvent {

	private static final long serialVersionUID = 2972600186154204917L;

	private Long piId, tiId;

	private Map<String, Object> variables;

	public TaskInstanceSubmitted(TaskInstanceW source, Long piId, Long tiId, Map<String, Object> variables) {
		super(source);

		this.piId = piId;
		this.tiId = tiId;

		this.variables = variables;
	}

	public Long getProcessInstanceId() {
		return piId;
	}

	public void setProcessInstanceId(Long piId) {
		this.piId = piId;
	}

	public Long getTaskInstanceId() {
		return tiId;
	}

	public void setTaskInstanceId(Long tiId) {
		this.tiId = tiId;
	}

	public Map<String, Object> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, Object> variables) {
		this.variables = variables;
	}

	@Override
	public String toString() {
		return "Task instance (ID: " + getTaskInstanceId() + ") submitted for process instance: " + getProcessInstanceId() + " with variables: " +
				getVariables();
	}
}