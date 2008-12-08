package com.idega.jbpm.view;

import java.util.Map;

public interface ViewSubmition {

	public abstract Long getTaskInstanceId();

	public abstract boolean isSubmitable();

	public abstract void populateVariables(Map<String, Object> variables);

	public abstract Map<String, Object> resolveVariables();

	public abstract void setSubmitable(boolean submitable);

	public abstract void setTaskInstanceId(Long taskInstanceId);

}