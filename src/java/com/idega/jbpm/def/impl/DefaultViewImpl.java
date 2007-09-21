package com.idega.jbpm.def.impl;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/21 11:29:39 $ by $Author: civilis $
 */
public class DefaultViewImpl implements View {

	private String viewId;
	protected String viewType;
	
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