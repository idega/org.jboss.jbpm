package com.idega.jbpm.exe;

import java.util.Date;

public interface BPMDocument {
	
	public abstract String getSubmittedByName();
	
	public abstract void setSubmittedByName(String submittedByName);
	
	public abstract String getDocumentName();
	
	public abstract void setDocumentName(String documentName);
	
	public abstract boolean isSignable();
	
	public abstract void setSignable(boolean signable);
	
	public abstract Long getTaskInstanceId();
	
	public abstract void setTaskInstanceId(Long taskInstanceId);
	
	public abstract String getAssignedToName();
	
	public abstract void setAssignedToName(String assignedToName);
	
	public abstract Date getCreateDate();
	
	public abstract void setCreateDate(Date createDate);
	
	public abstract Date getEndDate();
	
	public abstract void setEndDate(Date endDate);
	
	public abstract boolean isHasViewUI();
}