package com.idega.jbpm.exe.impl;

import java.util.Date;

import com.idega.jbpm.exe.BPMDocument;

/**
 * Represents document in bpm - this can be representation of task, or document (submitted task).
 * Represents document in the locale it was resolved for
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/03/27 15:33:14 $ by $Author: civilis $
 */
public class BPMDocumentImpl implements BPMDocument {
	
	private Long taskInstanceId;
	private String submittedByName;
	private String assignedToName;
	private String documentName;
	private Date createDate;
	private Date endDate;
	private boolean signable;
	private boolean hasViewUI = true;
	private Integer order;
	
	public String getSubmittedByName() {
		return submittedByName;
	}
	
	public void setSubmittedByName(String submittedByName) {
		this.submittedByName = submittedByName;
	}
	
	public String getDocumentName() {
		return documentName;
	}
	
	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}
	
	public boolean isSignable() {
		return signable;
	}
	
	public void setSignable(boolean signable) {
		this.signable = signable;
	}
	
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}
	
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	public String getAssignedToName() {
		return assignedToName;
	}
	
	public void setAssignedToName(String assignedToName) {
		this.assignedToName = assignedToName;
	}
	
	public Date getCreateDate() {
		return createDate;
	}
	
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isHasViewUI() {
    	return hasViewUI;
    }

	public void setHasViewUI(boolean hasViewUI) {
    	this.hasViewUI = hasViewUI;
    }

	public void setOrder(Integer order) {
		this.order = order;
	}

	public Integer getOrder() {
		return order;
	}
}