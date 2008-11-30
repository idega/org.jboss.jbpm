package com.idega.jbpm.identity;

import java.security.Permission;
import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * <p>
 * roles, actors managment, permissions in one piece
 * TODO: split
 * </p>
 * 
 * Recommended getting rolesManager from BPMFactory
 *   
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $
 * 
 * Last modified: $Date: 2008/11/30 08:17:03 $ by $Author: civilis $
 */
public interface RolesManager {

	public abstract void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId);
	
	public abstract void removeIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId);
	
	public abstract void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void hasRightsToAssignTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void checkPermission(Permission permission) throws BPMAccessControlException;
	
	public abstract Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId);
	
	public abstract Collection<User> getAllUsersForRoles(Collection<String> rolesNames, long piId, BPMTypedPermission perm);
	
	public abstract void createNativeRolesFromProcessRoles(String processName, Collection<Role> roles);
	
	public abstract void createTaskRolesPermissions(Task task, List<Role> roles);
	
	/**
	 * creates actors for process roles provided, for given process instance
	 * 
	 * @param roles
	 * @param processInstance
	 * @return actors created
	 */
	public abstract List<Actor> createProcessActors(Collection<Role> roles, ProcessInstance processInstance);
	
	public abstract void assignTaskRolesPermissions(Task task, List<Role> roles, Long processInstanceId);
	
	public abstract void assignRolesPermissions(List<Role> roles, ProcessInstance processInstance);

	/**
	 * taskInstance could be not from the same processInstance, but we should provide roles from both, with the precedence of task instance process instance
	 * @param processInstanceId
	 * @param taskInstanceId
	 * @return
	 */
	public abstract List<Actor> getProcessRolesForProcessInstanceByTaskInstance(Long processInstanceId, Long taskInstanceId, String processRoleName);
	
	public abstract List<Long> getProcessInstancesIdsForCurrentUser();
	
	public abstract List<Actor> getProcessRoles(Collection<String> rolesNames, Long processInstanceId);

	/**
	 * creates or updates task instance scope permissions for role.
	 * @param role - role object, containing role name, and accesses to set
	 * @param processInstanceId
	 * @param taskInstanceId
	 * @param setSameForAttachments - set the same access rights for binary variables of the task instance 
	 * @param variableName - if provided, set rights for variable for that task instance. This is usually used for task attachments.
	 */
	public abstract void setTaskRolePermissionsTIScope(Role role, Long processInstanceId, Long taskInstanceId, boolean setSameForAttachments, String variableName);
	
	public abstract void setContactsPermission(Role role, Long processInstanceId, Integer userId);
	
	public abstract List<Role> getRolesPermissionsForTaskInstance(Long processInstanceId, Long taskInstanceId, String variableName);
	
	public abstract Collection<Role> getUserPermissionsForRolesContacts(Long processInstanceId, Integer userId);
	
	public abstract List<Long> getProcessInstancesIdsForUser(IWContext iwc, User user, boolean checkIfSuperAdmin);
	
	public abstract List<String> getRolesForAccess(long processInstanceId, Access access);
	
	public abstract boolean checkFallsInRole(String roleName, List<NativeIdentityBind> nativeIdentities, int userId, AccessController ac, IWApplicationContext iwac);
	
	public abstract List<Role> getUserRoles(long processInstanceId, User user);
	
	public abstract PermissionsFactory getPermissionsFactory();
}