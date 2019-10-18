package com.idega.jbpm.event;

import java.io.Serializable;
import java.util.Map;

import org.springframework.context.ApplicationEvent;

import com.idega.jbpm.exe.TaskInstanceW;

public class TaskInstanceSubmitted extends ApplicationEvent {

	private static final long serialVersionUID = 2972600186154204917L;

	private Integer caseId, submittedBy;

	private Serializable piId, tiId;

	private Map<String, ? extends Object> variables;

	public TaskInstanceSubmitted(TaskInstanceW source, Serializable piId, Serializable tiId, Map<String, ? extends Object> variables) {
		super(source);

		this.piId = piId;
		this.tiId = tiId;

		this.variables = variables;
	}

	public <T extends Serializable> T getProcessInstanceId() {
		@SuppressWarnings("unchecked")
		T result = (T) piId;
		return result;
	}

	public void setProcessInstanceId(Serializable piId) {
		this.piId = piId;
	}

	public <T extends Serializable> T getTaskInstanceId() {
		@SuppressWarnings("unchecked")
		T result = (T) tiId;
		return result;
	}

	public void setTaskInstanceId(Serializable tiId) {
		this.tiId = tiId;
	}

	public Map<String, ? extends Object> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, Object> variables) {
		this.variables = variables;
	}

	public Integer getCaseId() {
		return caseId;
	}

	public void setCaseId(Integer caseId) {
		this.caseId = caseId;
	}

	public Integer getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(Integer submittedBy) {
		this.submittedBy = submittedBy;
	}

	@Override
	public String toString() {
		return "Task instance (ID: " + getTaskInstanceId() + ") submitted for process instance: " + getProcessInstanceId() + " with variables: " +
				getVariables();
	}
}