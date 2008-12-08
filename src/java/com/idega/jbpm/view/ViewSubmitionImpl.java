package com.idega.jbpm.view;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;


public class ViewSubmitionImpl implements ViewSubmition {

	private boolean submitable = true;
	private Long taskInstanceId;
	private Map<String, Object> variables;
	
	

	/* (non-Javadoc)
	 * @see com.idega.jbpm.view.ViewSubmition#getTaskInstanceId()
	 */
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.view.ViewSubmition#isSubmitable()
	 */
	public boolean isSubmitable() {
		
		return submitable;
	}


	/* (non-Javadoc)
	 * @see com.idega.jbpm.view.ViewSubmition#populateVariables(java.util.Map)
	 */
	public void populateVariables(Map<String, Object> variables) {
		this.variables = variables;
		
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.view.ViewSubmition#resolveVariables()
	 */
	public Map<String, Object> resolveVariables() {
		
		return variables;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.view.ViewSubmition#setSubmitable(boolean)
	 */
	public void setSubmitable(boolean submitable) {
		this.submitable = submitable;
		
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.view.ViewSubmition#setTaskInstanceId(java.lang.Long)
	 */
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
		
	}

}
