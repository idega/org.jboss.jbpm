package com.idega.jbpm.utils;

import com.idega.idegaweb.IWMainApplication;

public class JBPMUtil {

	private JBPMUtil() {}

	public static boolean isPerformanceMeasurementOn() {
		try {
			return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.measure_performance", Boolean.FALSE);
		} catch (Exception e) {}
		return Boolean.FALSE;
	}

	public static boolean isAlwaysFullAccess() {
		try {
			return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.skip_full_access_check", true);
		} catch (Exception e) {}
		return Boolean.FALSE;
	}

}