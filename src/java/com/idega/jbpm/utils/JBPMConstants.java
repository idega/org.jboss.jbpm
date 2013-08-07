package com.idega.jbpm.utils;

import java.util.logging.Logger;

import com.idega.util.CoreConstants;

public class JBPMConstants {

	public static final String	BPM_PATH = CoreConstants.PATH_FILES_ROOT + "/bpm",
								VARIABLE_LOCALIZATION_PREFIX = "bpm_variable.",
								PATH_IN_REPOSITORY = "pathInRepository",
								OVERWRITE = "overwrite";


	public static final Logger bpmLogger = Logger.getLogger("IdegaBPMLogging");
}