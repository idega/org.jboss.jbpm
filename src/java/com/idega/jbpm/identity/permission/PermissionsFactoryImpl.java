package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 *          Last modified: $Date: 2009/02/13 17:27:48 $ by $Author: civilis $
 */
@Service
@Scope("singleton")
public class PermissionsFactoryImpl implements PermissionsFactory {

	public static final String genericAccessPermType = "genericAccess";
	public static final String accessManagementPermType = "accessManagement";
	public static final String submitTaskParametersPermType = "submitTaskParameters";
	public static final String viewTaskInstancePermType = "viewTaskInstancePerm";
	public static final String viewTaskInstanceVariablePermType = "viewTaskInstanceVariablePerm";
	public static final String roleAccessPermType = "roleAccess";

	@Override
	public Permission getTaskInstanceSubmitPermission(Boolean authPooledActorsOnly, Long taskInstanceId) {
		BPMTypedPermission perm = getTypedPermission(submitTaskParametersPermType);
		perm.setAttribute(TaskAccessPermissionsHandler.checkOnlyInActorsPoolAtt, authPooledActorsOnly);
		perm.setAttribute(TaskAccessPermissionsHandler.taskInstanceAtt, taskInstanceId);

		ArrayList<Access> accesses = new ArrayList<Access>(2);
		accesses.add(Access.read);
		accesses.add(Access.write);

		perm.setAttribute(TaskAccessPermissionsHandler.accessesWantedAtt, accesses);

		return (Permission) perm;
	}

	@Override
	public Permission getRightsMgmtPermission(Long processInstanceId) {

		BPMTypedPermission perm = getTypedPermission(accessManagementPermType);
		perm.setAttribute(
				AccessManagementPermissionsHandler.processInstanceIdAtt,
				processInstanceId);

		return (Permission) perm;
	}

	@Override
	public Permission getAccessPermission(Long processInstanceId, Access access) {

		BPMTypedPermission perm = getTypedPermission(genericAccessPermType);
		perm.setAttribute(GenericAccessPermissionsHandler.processInstanceIdAtt,
				processInstanceId);

		return (Permission) perm;
	}

	@Override
	public Permission getAccessPermission(Long processInstanceId,
			Access access, User user) {

		BPMTypedPermission perm = getTypedPermission(genericAccessPermType);
		perm.setAttribute(GenericAccessPermissionsHandler.processInstanceIdAtt,
				processInstanceId);

		if (user != null)
			perm.setAttribute(GenericAccessPermissionsHandler.userAtt,
					new Integer(user.getPrimaryKey().toString()));

		return (Permission) perm;
	}

	@Override
	@Transactional(readOnly = true)
	public Permission getTaskInstanceViewPermission(Boolean authPooledActorsOnly, Long taskInstanceId) {
		BPMTypedPermission perm = getTypedPermission(viewTaskInstancePermType);
		perm.setAttribute(TaskAccessPermissionsHandler.checkOnlyInActorsPoolAtt, authPooledActorsOnly);
		perm.setAttribute(TaskAccessPermissionsHandler.taskInstanceAtt, taskInstanceId);

		ArrayList<Access> accesses = new ArrayList<Access>(1);
		accesses.add(Access.read);

		perm.setAttribute(TaskAccessPermissionsHandler.accessesWantedAtt, accesses);

		return (Permission) perm;
	}

	@Override
	public Permission getTaskInstanceVariableViewPermission(Boolean authPooledActorsOnly, TaskInstance taskInstance, String variableIdentifier) {
		BPMTypedPermission perm = getTypedPermission(viewTaskInstanceVariablePermType);
		perm.setAttribute(TaskAccessPermissionsHandler.checkOnlyInActorsPoolAtt, authPooledActorsOnly);
		perm.setAttribute(TaskAccessPermissionsHandler.taskInstanceAtt, taskInstance.getId());
		perm.setAttribute(TaskAccessPermissionsHandler.variableIdentifierAtt, variableIdentifier);

		List<Access> accesses = new ArrayList<Access>(1);
		accesses.add(Access.read);
		perm.setAttribute(TaskAccessPermissionsHandler.accessesWantedAtt, accesses);

		return (Permission) perm;
	}

	@Override
	public Permission getRoleAccessPermission(Long processInstanceId,
			String roleName, Boolean checkContactsForRole) {

		BPMTypedPermission perm = getTypedPermission(roleAccessPermType);
		perm.setAttribute(RoleAccessPermissionsHandler.processInstanceIdAtt,
				processInstanceId);
		perm.setAttribute(RoleAccessPermissionsHandler.roleNameAtt, roleName);
		perm.setAttribute(RoleAccessPermissionsHandler.checkContactsForRoleAtt,
				checkContactsForRole);

		return (Permission) perm;
	}

	@Override
	public BPMTypedPermission getTypedPermission(String type) {

		return new BPMTypedPermissionImpl(type, null);
	}
}