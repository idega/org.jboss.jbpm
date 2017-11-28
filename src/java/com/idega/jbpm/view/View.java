package com.idega.jbpm.view;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $ Last modified: $Date: 2009/03/27 15:47:21 $ by $Author: civilis $
 */
public interface View {

	// TODO: create view submission interface with method that are needed only on submit
	public abstract <T> void setTaskInstanceId(T taskInstanceId);

	public abstract <T> T getTaskInstanceId();

	public abstract void setViewId(String viewId);

	public abstract String getViewId();

	public abstract String getDisplayName();

	public abstract String getDisplayName(Locale locale);

	public abstract String getDefaultDisplayName();

	public abstract void setViewType(String viewType);

	public abstract String getViewType();

	/**
	 * @return if the getViewForDisplay() has implementation - i.e. would return the UIComponent
	 */
	public abstract boolean hasViewForDisplay();

	public abstract UIComponent getViewForDisplay();

	public abstract UIComponent getViewForDisplay(boolean pdfViewer);

	public abstract boolean isSubmitable();

	public abstract void setSubmitable(boolean submitable);

	public void setSubmitted(boolean submitted);
	public boolean isSubmitted();

	public abstract boolean populateVariables(Map<String, Object> variables);

	public abstract void populateParameters(Map<String, String> parameters);

	public abstract Map<String, Object> resolveVariables();

	public abstract Map<String, String> resolveParameters();

	public abstract Date getDateCreated();

	public abstract void takeView();

	public abstract ViewToTask getViewToTask();
}