package com.idega.jbpm.view;

import java.util.Map;

public class ViewSubmissionImpl implements ViewSubmission {

	private Map<String, Object> variables;
	private Long taskInstanceId;
	private Map<String, String> parameters;

	public void populateVariables(Map<String, Object> variables) {
		this.variables = variables;

	}

	public Map<String, Object> resolveVariables() {
		return variables;
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public Map<String, String> resolveParameters() {
		return parameters;
	}

	public void populateParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
}