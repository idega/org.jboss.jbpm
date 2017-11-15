package com.idega.jbpm.view;

import java.io.Serializable;
import java.util.Map;

public class ViewSubmissionImpl implements ViewSubmission {

	private Map<String, Object> variables;

	private Serializable taskInstanceId, piId, processDefinitionId;

	private Map<String, String> parameters;

	private String viewId, viewType;

	private boolean allowedToEdit;

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
	public <T extends Serializable> T getTaskInstanceId() {
		@SuppressWarnings("unchecked")
		T result = (T) taskInstanceId;
		return result;
	}

	@Override
	public <T extends Serializable> void setTaskInstanceId(T taskInstanceId) {
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
	public <T extends Serializable> T getProcessDefinitionId() {
		@SuppressWarnings("unchecked")
		T result = (T) processDefinitionId;
		return result;
	}

	@Override
	public <T extends Serializable> void setProcessDefinitionId(T processDefinitionId) {
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

	@Override
	public <T extends Serializable> void setProcessInstanceId(T piId) {
		this.piId = piId;
	}

	@Override
	public <T extends Serializable> T getProcessInstanceId() {
		@SuppressWarnings("unchecked")
		T result = (T) piId;
		return result;
	}

	@Override
	public boolean isAllowedToEdit() {
		return allowedToEdit;
	}

	public void setAllowedToEdit(boolean allowedToEdit) {
		this.allowedToEdit = allowedToEdit;
	}

}