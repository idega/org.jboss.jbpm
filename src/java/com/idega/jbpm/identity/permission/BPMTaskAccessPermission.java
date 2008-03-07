package com.idega.jbpm.identity.permission;

import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/07 13:26:41 $ by $Author: civilis $
 */
public interface BPMTaskAccessPermission {

	public abstract List<Access> getAccesses();
	
	public TaskInstance getTaskInstance();
}