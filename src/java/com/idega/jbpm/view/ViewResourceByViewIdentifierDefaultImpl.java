package com.idega.jbpm.view;

import java.io.IOException;

import com.idega.idegaweb.IWMainApplication;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/01/25 15:36:31 $ by
 *          $Author: civilis $
 */
public class ViewResourceByViewIdentifierDefaultImpl implements ViewResource {

	private String taskName;
	private String viewResourceIdentifier;
	private String viewType;
	private Integer order;

	public void store(IWMainApplication iwma) throws IOException {

		// does nothing in this implementation
	}

	public void setProcessName(String processName) {
	}

	public void setViewResourceIdentifier(String viewResourceIdentifier) {
		this.viewResourceIdentifier = viewResourceIdentifier;
	}

	public String getViewResourceIdentifier() {
		return viewResourceIdentifier;
	}

	public String getViewType() {
		return viewType;
	}

	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	public String getViewId() {

		// viewId the same as viewResourceIdentifier in this implementation
		return getViewResourceIdentifier();
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public Integer getOrder() {
		return order;
	}
}