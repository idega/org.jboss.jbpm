package com.idega.jbpm.exe;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/09/21 11:29:39 $ by $Author: civilis $
 */
public interface SubmissionHandler {

	public abstract void submit(TaskInstance ti, Object submissionData);
	public abstract Object populate(TaskInstance ti, Object objectToPopulate);
	public abstract String getIdentifier();
}