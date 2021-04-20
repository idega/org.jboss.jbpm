package com.idega.jbpm.view;

import java.io.Serializable;
import java.util.Map;

import com.idega.presentation.IWContext;

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

	public abstract <T extends Serializable> void setProcessInstanceId(T piId);

	public abstract <T extends Serializable> T getProcessInstanceId();

	public abstract boolean isAllowedToEdit();

	public abstract void setContext(IWContext iwc);

	public abstract IWContext getContext();

}