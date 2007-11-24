package com.idega.jbpm.presentation.xml;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/24 18:26:12 $ by $Author: civilis $
 */
public class ProcessArtifactsListRow {

	static final String alias = "row";
	
	static final String attributeId = "id";
	private String id;
	
	static final String implicitCells = "cells";
	static final String implicitAkaCells = "cell";
	private List<String> cells = new ArrayList<String>();
	
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
		getCells().add(cellContent);
	}
}