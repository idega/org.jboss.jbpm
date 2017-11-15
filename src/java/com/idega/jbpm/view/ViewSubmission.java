package com.idega.jbpm.view;

import java.io.Serializable;
import java.util.Map;

/**
 * meant to be used for resolving submission variables
 */
public interface ViewSubmission {

	public abstract void populateVariables(Map<String, ? extends Object> variables);

	public abstract Map<String, Object> resolveVariables();

	public abstract Map<String, String> resolveParameters();

	public abstract void populateParameters(Map<String, String> parameters);

	public abstract <T extends Serializable> T getTaskInstanceId();

	public abstract <T extends Serializable> void setTaskInstanceId(T taskInstanceId);

	public abstract <T extends Serializable> T getProcessDefinitionId();

	public abstract <T extends Serializable> void setProcessDefinitionId(T processDefinitionId);

	public abstract String getViewId();

	public abstract void setViewId(String viewId);

	public abstract String getViewType();

	public abstract void setViewType(String viewType);

	public <T extends Serializable> void setProcessInstanceId(T piId);

	public <T extends Serializable> T getProcessInstanceId();

	public boolean isAllowedToEdit();

}