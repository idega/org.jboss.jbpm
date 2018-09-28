package com.idega.jbpm.utils;

import java.util.logging.Logger;

import com.idega.util.CoreConstants;

public class JBPMConstants {

	public static final String	BPM_PATH = CoreConstants.PATH_FILES_ROOT + "/bpm",
								VARIABLE_LOCALIZATION_PREFIX = "bpm_variable.",

								PATH_IN_REPOSITORY = "pathInRepository",
								OVERWRITE = "overwrite";

	public static final Logger bpmLogger = Logger.getLogger("IdegaBPMLogging");

	public static final String SOURCE = "source";

	public static final String VISIBLE_CONTACTS_GROUP_NAME = "Sýnilegur";
	public static final String VISIBLE_CONTACTS_ROLE_NAME = "bpm_visible_contacts";

	public static final String APP_PROP_SHOW_ONLY_SELECTED_CONTACTS = "bpm.show_only_selected_contacts";
}