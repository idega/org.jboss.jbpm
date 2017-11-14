package com.idega.jbpm.identity.permission;

import java.io.Serializable;
import java.security.Permission;

import com.idega.user.data.User;

/**
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 *          Last modified: $Date: 2009/02/13 17:27:48 $ by $Author: civilis $
 */
public interface PermissionsFactory {

	public abstract <T extends Serializable> Permission getTaskInstanceSubmitPermission(Boolean authPooledActorsOnly, T taskInstanceId);

	public abstract <T extends Serializable> Permission getRightsMgmtPermission(T processInstanceId);

	public abstract <T extends Serializable> Permission getAccessPermission(T processInstanceId, Access access);

	public abstract <T extends Serializable> Permission getAccessPermission(T processInstanceId, Access access, User user);

	public abstract <T extends Serializable> Permission getTaskInstanceViewPermission(Boolean authPooledActorsOnly, T taskInstanceId);

	public abstract <T extends Serializable> Permission getTaskInstanceVariableViewPermission( Boolean authPooledActorsOnly, T taskInstanceId, String variableIdentifier);

	/**
	 *
	 * @param processInstanceId
	 * @param roleName
	 * @param checkContactsForRole
	 *            - if current user can see contacts of the role provided. if
	 *            set to false, basic falling to role is checked
	 * @return
	 */
	public abstract <T extends Serializable> Permission getRoleAccessPermission(T processInstanceId, String roleName, Boolean checkContactsForRole);

	public abstract BPMTypedPermission getTypedPermission(String name);
}