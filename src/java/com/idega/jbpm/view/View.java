package com.idega.jbpm.view;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/07/11 14:17:39 $ by $Author: anton $
 */
public interface View {

	public abstract void setTaskInstanceId(Long taskInstanceId);
	public abstract Long getTaskInstanceId();
	
	public abstract void setViewId(String viewId);
	public abstract String getViewId();
	
	public abstract String getDisplayName();
	public abstract String getDisplayName(Locale locale);
	public abstract String getDefaultDisplayName();
	
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