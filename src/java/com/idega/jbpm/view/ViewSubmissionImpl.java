package com.idega.jbpm.view;

import java.util.Map;

public class ViewSubmissionImpl implements ViewSubmission {

	private Map<String, Object> variables;
	private Long taskInstanceId;
	private Long processDefinitionId;
	private Map<String, String> parameters;
	private String viewId;
	private String viewType;

	@Override
	@SuppressWarnings("unchecked")
	public void populateVariables(Map<String, ? extends Object> variables) {
		this.variables = (Map<String, Object>) variables;
	}

	@Override
	public Map<String, Object> resolveVariables() {
		return variables;
	}

	@Override
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	@Override
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	public Map<String, String> resolveParameters() {
		return parameters;
	}

	@Override
	public void populateParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	@Override
	public void setProcessDefinitionId(Long processDefinitionId) {
		this.processDefinitionId = processDefinitionId;
	}

	@Override
	public String getViewId() {
		return viewId;
	}

	@Override
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}

	@Override
	public String getViewType() {
		return viewType;
	}

	@Override
	public void setViewType(String viewType) {
		this.viewType = viewType;
	}
}