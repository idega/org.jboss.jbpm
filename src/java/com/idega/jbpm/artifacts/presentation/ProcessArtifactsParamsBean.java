package com.idega.jbpm.artifacts.presentation;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/09/26 15:04:21 $ by $Author: valdas $
 */
public class ProcessArtifactsParamsBean {

	private Integer page;
	private Integer rows;
	private String sidx;
	private String sord;
	private Long piId;
	private Long taskId;
	private boolean rightsChanger = false;
	private String identifier;
	private Boolean downloadDocument;
	private Boolean allowPDFSigning = Boolean.TRUE;
	
	public Integer getPage() {
		return page;
	}
	public void setPage(Integer page) {
		this.page = page;
	}
	public Integer getRows() {
		return rows;
	}
	public void setRows(Integer rows) {
		this.rows = rows;
	}
	public String getSidx() {
		return sidx;
	}
	public void setSidx(String sidx) {
		this.sidx = sidx;
	}
	public String getSord() {
		return sord;
	}
	public void setSord(String sord) {
		this.sord = sord;
	}
	public Long getPiId() {
		return piId;
	}
	public void setPiId(Long piId) {
		this.piId = piId;
	}
	public Long getTaskId() {
		return taskId;
	}
	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}
	public boolean isRightsChanger() {
		return rightsChanger;
	}
	public void setRightsChanger(boolean rightsChanger) {
		this.rightsChanger = rightsChanger;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public Boolean getDownloadDocument() {
		return downloadDocument == null ? true : downloadDocument;
	}
	public void setDownloadDocument(Boolean downloadDocument) {
		this.downloadDocument = downloadDocument;
	}
	public Boolean getAllowPDFSigning() {
		return allowPDFSigning;
	}
	public void setAllowPDFSigning(Boolean allowPDFSigning) {
		this.allowPDFSigning = allowPDFSigning;
	}
}