package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 *          Last modified: $Date: 2009/02/25 13:10:51 $ by $Author: civilis $
 */
public final class ProcessConstants {

	private ProcessConstants() {
	}

	public static final String START_PROCESS = "startProcess";
	public static final String TASK_INSTANCE_ID = "taskInstanceId";
	public static final String PROCESS_DEFINITION_ID = "processDefinitionId";
	public static final String VIEW_ID = "viewId";
	public static final String VIEW_TYPE = "viewType";

	/**
	 * shared task means it can be opened by multiple users at the same time.
	 * New tokens and task instances will be created for each user
	 */
	public static final int PRIORITY_SHARED_TASK = -1;

	public static final String	actionTakenVariableName = "string_actionTaken",
								OWNER_EMAIL_ADDRESS = "string_ownerEmailAddress";

	public static final String mainProcessInstanceIdVariableName = "mainProcessInstanceId";
}