package com.idega.jbpm.presentation.xml;

import java.util.ArrayList;
import java.util.List;

import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/06/19 09:50:17 $ by $Author: civilis $
 */
public class ProcessArtifactsListRow {

	private Integer order;
	
	static final String alias = "row";
	
	static final String attributeId = "id";
	static final String attributeStyleClass = "styleClass";
	static final String attributeDisabledSelection = "disabledSelection";
	
	private static final String trueValue = "true";
	private String id;
	private String styleClass;
	private String disabledSelection;
	
	static final String implicitCells = "cells";
	static final String implicitAkaCells = "cell";
	private List<String> cells = new ArrayList<String>();
	
	private int dateCellIndex = -1;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<String> getCells() {
		return cells;
	}
	public void addCell(String cellContent) {
		getCells().add(cellContent == null ? CoreConstants.EMPTY : cellContent);
	}
	public String getStyleClass() {
		return styleClass;
	}
	public void setStyleClass(String styleClass) {
		this.styleClass = styleClass;
	}
	
	public void setDisabledSelection(boolean disabledSelection) {
		this.disabledSelection = disabledSelection ? trueValue : null;
	}
	
	public boolean isDisabledSelection() {
		return trueValue.equals(disabledSelection);
	}
	protected String getDisabledSelection() {
		return disabledSelection;
	}
	protected void setDisabledSelection(String disabledSelection) {
		this.disabledSelection = disabledSelection;
	}
	public int getDateCellIndex() {
		return dateCellIndex;
	}
	public void setDateCellIndex(int dateCellIndex) {
		this.dateCellIndex = dateCellIndex;
	}
	public Integer getOrder() {
		return order;
	}
	public void setOrder(Integer order) {
		this.order = order;
	}
	
}