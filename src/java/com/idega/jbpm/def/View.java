package com.idega.jbpm.def;

import javax.faces.component.UIComponent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public interface View {

	public abstract void setViewId(String viewId);
	public abstract String getViewId();
	
	public abstract void setViewType(String viewType);
	public abstract String getViewType();
	
	public abstract UIComponent getViewForDisplay(Long taskInstanceId);
}