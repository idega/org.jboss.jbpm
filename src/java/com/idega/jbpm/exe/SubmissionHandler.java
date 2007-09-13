package com.idega.jbpm.exe;

import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:24 $ by $Author: civilis $
 */
public interface SubmissionHandler {

	public abstract void submit(TaskInstance ti, Object submissionData);
	public abstract Object populate(TaskInstance ti, Object objectToPopulate);
	public abstract String getIdentifier();
}