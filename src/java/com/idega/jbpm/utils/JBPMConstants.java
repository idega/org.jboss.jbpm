package com.idega.jbpm.utils;

import java.util.logging.Logger;

import com.idega.util.CoreConstants;

public class JBPMConstants {

	public static final String BPM_PATH = CoreConstants.PATH_FILES_ROOT
			+ "/bpm";
	public static final String BPM_COMMENTS_PATH = BPM_PATH + "/comments/";

	public static final Logger bpmLogger = Logger.getLogger("IdegaBPMLogging");
}