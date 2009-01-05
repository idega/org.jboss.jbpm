package com.idega.jbpm.exe.impl;

import java.util.Date;

import com.idega.jbpm.exe.BPMDocument;

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
 *          Last modified: $Date: 2009/01/05 04:06:10 $ by $Author: juozas $
 */
public class BPMDocumentImpl implements BPMDocument {

	private Long taskInstanceId;
	private String submittedByName;
	private String assignedToName;
	private String documentName;
	private Date createDate;
	private Date endDate;
	private boolean signable;

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#getSubmittedByName()
	 */
	public String getSubmittedByName() {
		return submittedByName;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setSubmittedByName(java.lang.String)
	 */
	public void setSubmittedByName(String submittedByName) {
		this.submittedByName = submittedByName;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#getDocumentName()
	 */
	public String getDocumentName() {
		return documentName;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setDocumentName(java.lang.String)
	 */
	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#isSignable()
	 */
	public boolean isSignable() {
		return signable;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setSignable(boolean)
	 */
	public void setSignable(boolean signable) {
		this.signable = signable;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#getTaskInstanceId()
	 */
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setTaskInstanceId(java.lang.Long)
	 */
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#getAssignedToName()
	 */
	public String getAssignedToName() {
		return assignedToName;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setAssignedToName(java.lang.String)
	 */
	public void setAssignedToName(String assignedToName) {
		this.assignedToName = assignedToName;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#getCreateDate()
	 */
	public Date getCreateDate() {
		return createDate;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setCreateDate(java.util.Date)
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#getEndDate()
	 */
	public Date getEndDate() {
		return endDate;
	}

	/* (non-Javadoc)
	 * @see com.idega.jbpm.exe.BPMDocument#setEndDate(java.util.Date)
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}