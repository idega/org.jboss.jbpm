package com.idega.jbpm.identity.permission;

import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/16 19:00:30 $ by $Author: civilis $
 */
public interface BPMTaskAccessPermission {

	public abstract List<Access> getAccesses();
	
	public abstract TaskInstance getTaskInstance();
	
	public abstract boolean getCheckOnlyInActorsPool();
}