package com.idega.jbpm.presentation.xml;

import java.text.DateFormat;
import java.util.Comparator;
import java.util.Locale;

import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;

public class ProcessArtifactsListRowComparator implements Comparator<ProcessArtifactsListRow> {

	private int dateFormat = -1;
	private Locale locale = null;
	private DateFormat dateFormatter = null;
	
	public ProcessArtifactsListRowComparator(int dateFormat, Locale locale) {
		this.dateFormat = dateFormat;
		this.locale = locale;
	}
	
	public int compare(ProcessArtifactsListRow row1, ProcessArtifactsListRow row2) {
		if (dateFormat < 0 || locale == null) {
			return 0;
		}
		
		if (row1.getDateCellIndex() < 0 || row2.getDateCellIndex() < 0) {
			return 0;
		}
		
		String timestampCellValue1 = null;
		String timestampCellValue2 = null;
		
		try {
			timestampCellValue1 = row1.getCells().get(row1.getDateCellIndex());
			timestampCellValue2 = row2.getCells().get(row2.getDateCellIndex());
		} catch(IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		if (timestampCellValue1 == null || timestampCellValue2 == null || CoreConstants.EMPTY.equals(timestampCellValue1) || CoreConstants.EMPTY.equals(timestampCellValue2)) {
			return 0;
		}
		
		if (dateFormatter == null) {
			dateFormatter = DateFormat.getDateInstance(dateFormat, locale);
		}
		
		IWTimestamp time1 = null;
		IWTimestamp time2 = null;
		try {
			time1 = new IWTimestamp(dateFormatter.parse(timestampCellValue1));
			time2 = new IWTimestamp(dateFormatter.parse(timestampCellValue2));
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (time1 == null || time2 == null) {
			return 0;
		}
		
		return time1.isEarlierThan(time2) ? 1 : 0;
	}

}
