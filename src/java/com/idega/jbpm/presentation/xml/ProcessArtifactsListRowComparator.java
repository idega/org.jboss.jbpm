package com.idega.jbpm.presentation.xml;

import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import com.idega.util.StringUtil;

public class ProcessArtifactsListRowComparator implements Comparator<ProcessArtifactsListRow> {

	private int dateStyle = -1;
	private int timeStyle = -1;
	private Locale locale = null;
	private DateFormat dateFormatter = null;
	
	public ProcessArtifactsListRowComparator(int dateStyle, int timeStyle, Locale locale) {
		this.dateStyle = dateStyle;
		this.timeStyle = timeStyle;
		this.locale = locale;
	}
	
	public int compare(ProcessArtifactsListRow row1, ProcessArtifactsListRow row2) {
		Integer order1 = row1.getOrder();
		Integer order2 = row2.getOrder();
		if (order1 != null && order2 != null) {
			return order1.compareTo(order2);
		}
		
		if (dateStyle < 0 || timeStyle < 0 || locale == null) {
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
		if (StringUtil.isEmpty(timestampCellValue1) || StringUtil.isEmpty(timestampCellValue2)) {
			return 0;
		}
		
		if (dateFormatter == null) {
			dateFormatter = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
		}
		
		Date time1 = null;
		Date time2 = null;
		try {
			time1 = dateFormatter.parse(timestampCellValue1);
			time2 = dateFormatter.parse(timestampCellValue2);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (time1 == null || time2 == null) {
			return 0;
		}

		return -time1.compareTo(time2);
	}

}