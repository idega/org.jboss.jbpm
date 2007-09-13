package com.idega.jbpm.def;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:24 $ by $Author: civilis $
 */
public class DefaultViewImpl implements View {

	private String viewId;
	
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}
	
	public String getViewId() {
		return viewId;
	}
}