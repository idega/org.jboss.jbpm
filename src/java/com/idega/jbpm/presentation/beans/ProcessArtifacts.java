package com.idega.jbpm.presentation.beans;

import org.w3c.dom.Document;

import com.idega.jbpm.presentation.xml.ProcessArtifactsListRow;
import com.idega.jbpm.presentation.xml.ProcessArtifactsListRows;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/24 18:26:12 $ by $Author: civilis $
 */
public class ProcessArtifacts {

	public Document processArtifactsList(ProcessArtifactsParamsBean params) {
		
		System.out.println("params got: "+params.getSord());

		ProcessArtifactsListRows rows = new ProcessArtifactsListRows();

		rows.setTotal(1);
		rows.setPage(1);
		rows.setRecords(1);
		
		ProcessArtifactsListRow row = new ProcessArtifactsListRow();
		row.setId("1");
		
		row.addCell("1");
		row.addCell("kaka");
		row.addCell("caca");
		row.addCell("103");
		row.addCell("taxis");
		row.addCell("545");
		row.addCell("some note");
		
		rows.addRow(row);
		
		row = new ProcessArtifactsListRow();
		row.setId("2");
		
		row.addCell("2");
		row.addCell("amama");
		row.addCell("avava");
		row.addCell("104");
		row.addCell("taxas");
		row.addCell("455");
		row.addCell("some note xx");
		
		rows.addRow(row);
		
		try {
			return rows.getDocument();
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}