package com.idega.jbpm.artifacts.presentation;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/27 15:32:33 $ by $Author: civilis $
 */
public class GridEntriesBean {
	
	private Long processInstanceId;
	private Document gridEntries;
	private Map<String, Object> rowsParams;
	
	public GridEntriesBean() {
	}
	
	public GridEntriesBean(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	@SuppressWarnings("unchecked")
	public void addRowParam(String rowId, String paramId, Object paramValue) {
		
		Map<String, Object> rowsParams = getRowsParams();
		Map<String, Object> rowParams;
		
		if (!rowsParams.containsKey(rowId)) {
			
			rowParams = new HashMap<String, Object>();
			rowsParams.put(rowId, rowParams);
		} else {
			rowParams = (Map<String, Object>) rowsParams.get(rowId);
		}
		
		rowParams.put(paramId, paramValue);
	}
	
	public void setRowHasViewUI(String rowId, boolean hasViewUI) {
		
		addRowParam(rowId, "hasViewUI", new Boolean(hasViewUI));
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}
	
	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public Document getGridEntries() {
		return gridEntries;
	}
	
	public void setGridEntries(Document gridEntries) {
		this.gridEntries = gridEntries;
	}
	
	public Map<String, Object> getRowsParams() {
		
		if (rowsParams == null)
			rowsParams = new HashMap<String, Object>();
		
		return rowsParams;
	}
	
	public void setRowsParams(Map<String, Object> rowsParams) {
		this.rowsParams = rowsParams;
	}
}