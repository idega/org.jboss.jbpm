package com.idega.jbpm.view;

import java.util.Date;
import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 13:52:40 $ by $Author: civilis $
 */
public interface View {

	public abstract void setTaskInstanceId(Long taskInstanceId);
	public abstract Long getTaskInstanceId();
	
	public abstract void setViewId(String viewId);
	public abstract String getViewId();
	
	public abstract String getDisplayName();
	
	public abstract void setViewType(String viewType);
	public abstract String getViewType();
	
	public abstract UIComponent getViewForDisplay();
	
	public abstract boolean isSubmitable();
	public abstract void setSubmitable(boolean submitable);
	
	public abstract void populateVariables(Map<String, Object> variables);
	public abstract void populateParameters(Map<String, String> parameters);
	
	public abstract Map<String, Object> resolveVariables();
	public abstract Map<String, String> resolveParameters();
	
	public abstract Date getDateCreated();
	
	public abstract void takeView();
	
	public abstract ViewToTask getViewToTask();
}