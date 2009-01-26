package com.idega.jbpm.view;

import java.util.Map;

public class ViewSubmissionImpl implements ViewSubmission {

	private Map<String, Object> variables;
	private Long taskInstanceId;
	private Long processDefinitionId;
	private Map<String, String> parameters;
	private String viewId;
	private String viewType;

	@SuppressWarnings("unchecked")
	public void populateVariables(Map<String, ? extends Object> variables) {
		this.variables = (Map<String, Object>)variables;
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

	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	public String getViewId() {
		return viewId;
	}

	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}
}