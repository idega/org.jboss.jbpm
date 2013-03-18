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

	private static final long serialVersionUID = 2032194549586333444L;

	private Long taskInstanceId;
	private String submittedByName;
	private String assignedToName;
	private String documentName;
	private Date createDate;
	private Date endDate;
	private boolean signable;
	private boolean hasViewUI = true;
	private Integer order;

	@Override
	public String getSubmittedByName() {
		return submittedByName;
	}

	@Override
	public void setSubmittedByName(String submittedByName) {
		this.submittedByName = submittedByName;
	}

	@Override
	public String getDocumentName() {
		return documentName;
	}

	@Override
	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	@Override
	public boolean isSignable() {
		return signable;
	}

	@Override
	public void setSignable(boolean signable) {
		this.signable = signable;
	}

	@Override
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	@Override
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	@Override
	public String getAssignedToName() {
		return assignedToName;
	}

	@Override
	public void setAssignedToName(String assignedToName) {
		this.assignedToName = assignedToName;
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	@Override
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@Override
	public Date getEndDate() {
		return endDate;
	}

	@Override
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Override
	public boolean isHasViewUI() {
    	return hasViewUI;
    }

	public void setHasViewUI(boolean hasViewUI) {
    	this.hasViewUI = hasViewUI;
    }

	@Override
	public void setOrder(Integer order) {
		this.order = order;
	}

	@Override
	public Integer getOrder() {
		return order;
	}
}