package com.idega.jbpm.identity;

import java.security.Permission;
import java.util.Collection;
import java.util.List;

import org.jbpm.taskmgmt.def.Task;

import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMRoleAccessPermission;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.23 $
 * 
 * Last modified: $Date: 2008/08/11 13:33:10 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId);
	
	public abstract void removeIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId);
	
	public abstract void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void hasRightsToAssignTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void checkPermission(Permission permission) throws BPMAccessControlException;
	
	public abstract Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId);
	
	public abstract Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId, BPMRoleAccessPermission perm);
	
	public abstract void createNativeRolesFromProcessRoles(String processName, Collection<Role> roles);
	
	public abstract void createTaskRolesPermissions(Task task, List<Role> roles);
	
	public abstract List<ProcessRole> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId);
	
	public abstract void assignTaskRolesPermissions(Task task, List<Role> roles, Long processInstanceId);
	
	public abstract void assignRolesPermissions(List<Role> roles, Long processInstanceId);

	/**
	 * taskInstance could be not from the same processInstance, but we should provide roles from both, with the precedence of task instance process instance
	 * @param processInstanceId
	 * @param taskInstanceId
	 * @return
	 */
	public abstract List<ProcessRole> getProcessRolesForProcessInstanceByTaskInstance(Long processInstanceId, Long taskInstanceId, String processRoleName);
	
	public abstract List<Long> getProcessInstancesIdsForCurrentUser();
	
	public abstract List<ProcessRole> getProcessRoles(Collection<String> rolesNames, Long processInstanceId);

	/**
	 * creates or updates task instance scope permissions for role.
	 * @param role - role object, containing role name, and accesses to set
	 * @param processInstanceId
	 * @param taskInstanceId
	 * @param setSameForAttachments - set the same access rights for binary variables of the task instance 
	 * @param variableName - if provided, set rights for variable for that task instance. This is usually used for task attachments.
	 */
	public abstract void setTaskRolePermissionsTIScope(Role role, Long processInstanceId, Long taskInstanceId, boolean setSameForAttachments, String variableName);
	
	public abstract List<Role> getRolesPermissionsForTaskInstance(Long processInstanceId, Long taskInstanceId, String variableName);
	
	public abstract List<Long> getProcessInstancesIdsForUser(IWContext iwc, User user, boolean checkIfSuperAdmin);
	
	public abstract List<String> getRolesForAccess(long processInstanceId, Access access);
}