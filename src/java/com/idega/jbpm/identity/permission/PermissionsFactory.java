package com.idega.jbpm.identity.permission;

import java.security.Permission;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.user.data.User;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/12/28 12:08:03 $ by $Author: civilis $
 */
public interface PermissionsFactory {

	public abstract Permission getTaskInstanceSubmitPermission(
			Boolean authPooledActorsOnly, TaskInstance taskInstance);

	public abstract Permission getRightsMgmtPermission(Long processInstanceId);

	public abstract Permission getAccessPermission(Long processInstanceId,
			Access access);
	
	public abstract Permission getAccessPermission(Long processInstanceId,
			Access access, User user);

	public abstract Permission getTaskViewPermission(
			Boolean authPooledActorsOnly, TaskInstance taskInstance);

	public abstract Permission getTaskVariableViewPermission(
			Boolean authPooledActorsOnly, TaskInstance taskInstance,
			String variableIdentifier);
	
	/**
	 * 
	 * @param processInstanceId
	 * @param roleName
	 * @param checkContactsForRole - if current user can see contacts of the role provided. if set to false, basic falling to role is checked
	 * @return
	 */
	public abstract Permission getRoleAccessPermission(Long processInstanceId, String roleName, Boolean checkContactsForRole);

	public abstract BPMTypedPermission getTypedPermission(String name);
}