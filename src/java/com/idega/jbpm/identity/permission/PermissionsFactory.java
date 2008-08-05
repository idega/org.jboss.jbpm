package com.idega.jbpm.identity.permission;

import java.security.Permission;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * TODO: implement new permissions management mechanism, something like:
 * have a properties based permission concept (for various values), and each permission would be of certain type (as it is of interface now),
 * and have the handler for each type available (which checks for permission satisfy).
 * Or even have an aspect, which would check each method, which is done with permission needed 
 * (permission, or permissions are being passed to the method params).
 * Would be like "do something with satisfying permission - throws exception if doesn't"
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/05 07:23:09 $ by $Author: civilis $
 */
public interface PermissionsFactory {

	public abstract Permission getTaskSubmitPermission(
			boolean authPooledActorsOnly, TaskInstance taskInstance);

	public abstract Permission getRightsMgmtPermission(Long processInstanceId);

	public abstract Permission getAccessPermission(long processInstanceId,
			Access access);

	public abstract Permission getTaskViewPermission(
			boolean authPooledActorsOnly, TaskInstance taskInstance);

	public abstract Permission getTaskVariableViewPermission(
			boolean authPooledActorsOnly, TaskInstance taskInstance,
			String variableIdentifier);
}