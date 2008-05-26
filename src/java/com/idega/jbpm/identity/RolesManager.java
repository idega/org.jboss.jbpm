package com.idega.jbpm.identity;

import java.security.Permission;
import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;

import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.user.data.User;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $
 * 
 * Last modified: $Date: 2008/05/26 11:03:16 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId);
	
	public abstract void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void hasRightsToAssignTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void checkPermission(Permission permission) throws BPMAccessControlException;
	
	public abstract Collection<User> getAllUsersForRoles(Collection<String> rolesNames, ProcessInstance pi);
	
	public abstract void createNativeRolesFromProcessRoles(String processName, Collection<Role> roles);
	
	public abstract void createTaskRolesPermissions(Task task, List<Role> roles);
	
	public abstract List<ProcessRole> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId);
	
	public abstract void assignTaskRolesPermissions(Task task, List<Role> roles, Long processInstanceId);

	public abstract List<ProcessRole> getProcessRolesForProcessInstanceByTaskInstance(Long taskInstanceId);
	
	public abstract List<Long> getProcessInstancesIdsForCurrentUser();
	
	public abstract List<ProcessRole> getProcessRoles(Collection<String> rolesNames, Long processInstanceId);

	/**
	 * creates or updates task instance scope permissions for role.
	 * @param role - role object, containing role name, and accesses to set
	 * @param taskInstanceId
	 * @param setSameForAttachments - set the same access rights for binary variables of the task instance 
	 * @param variableName - if provided, set rights for variable for that task instance. This is usually used for task attachments.
	 */
	public abstract void setTaskRolePermissionsTIScope(Role role, Long taskInstanceId, boolean setSameForAttachments, String variableName);
	
	public abstract List<Role> getRolesPermissionsForTaskInstance(Long taskInstanceId, String variableName);
}