package com.idega.jbpm.presentation.beans;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/03/29 20:28:24 $ by $Author: civilis $
 */
public class ProcessArtifactsParamsBean {

	private Integer page;
	private Integer rows;
	private String sidx;
	private String sord;
	private Long piId;
	private Long taskId;
	
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
}