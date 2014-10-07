package com.idega.jbpm.view;

import java.util.Map;

/**
 * meant to be used for resolving submission variables
 */
public interface ViewSubmission {

	public abstract void populateVariables(Map<String, ? extends Object> variables);

	public abstract Map<String, Object> resolveVariables();

	public abstract Map<String, String> resolveParameters();

	public abstract void populateParameters(Map<String, String> parameters);

	public abstract Long getTaskInstanceId();

	public abstract void setTaskInstanceId(Long taskInstanceId);

	public abstract Long getProcessDefinitionId();

	public abstract void setProcessDefinitionId(Long processDefinitionId);

	public abstract String getViewId();

	public abstract void setViewId(String viewId);

	public abstract String getViewType();

	public abstract void setViewType(String viewType);

	public void setProcessInstanceId(Long piId);

	public Long getProcessInstanceId();

}