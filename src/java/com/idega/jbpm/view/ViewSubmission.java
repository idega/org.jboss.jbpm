package com.idega.jbpm.view;

import java.util.Map;

/**
 * meant to be used for resolving submission variables
 */
public interface ViewSubmission {

	public abstract void populateVariables(Map<String, Object> variables);

	public abstract Map<String, Object> resolveVariables();

	public abstract Map<String, String> resolveParameters();

	public abstract void populateParameters(Map<String, String> parameters);

	public abstract Long getTaskInstanceId();

	public abstract void setTaskInstanceId(Long taskInstanceId);
}