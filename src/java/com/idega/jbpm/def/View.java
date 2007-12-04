package com.idega.jbpm.def;

import java.util.Map;

import javax.faces.component.UIComponent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2007/12/04 14:06:02 $ by $Author: civilis $
 */
public interface View {

	public abstract void setViewId(String viewId);
	public abstract String getViewId();
	
	public abstract void setViewType(String viewType);
	public abstract String getViewType();
	
	public abstract UIComponent getViewForDisplay();
	
	public abstract void populate(Map<String, Object> variables);
}