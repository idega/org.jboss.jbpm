package com.idega.jbpm.exe;

import java.util.Date;

/**
 * Represents document in bpm - this can be representation of task, or document
 * (submitted task).
 * 
 * Represents document in the locale it was resolved for
 * 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/12/28 12:08:03 $ by $Author: civilis $
 */
public class BPMDocumentImpl {

	private Long taskInstanceId;
	private String submittedByName;
	private String assignedToName;
	private String documentName;
	private Date createDate;
	private Date endDate;
	private boolean signable;

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
}