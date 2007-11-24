package com.idega.jbpm.presentation.beans;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/24 18:26:12 $ by $Author: civilis $
 */
public class ProcessArtifactsParamsBean {

	private Integer page;
	private Integer rows;
	private String sidx;
	private String sord;
	private Integer piId;
	
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
	public Integer getPiId() {
		return piId;
	}
	public void setPiId(Integer piId) {
		this.piId = piId;
	}
}