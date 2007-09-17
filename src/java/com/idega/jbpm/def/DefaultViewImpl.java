package com.idega.jbpm.def;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/09/17 13:33:39 $ by $Author: civilis $
 */
public class DefaultViewImpl implements View {

	private String viewId;
	private String viewType;
	
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}
	
	public String getViewId() {
		return viewId;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}
}