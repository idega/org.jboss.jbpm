package com.idega.jbpm.identity;

import java.security.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * <p>
 * roles, actors management, permissions in one piece TODO: split
 * </p>
 * Recommended getting rolesManager from BPMFactory
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.37 $ Last modified: $Date: 2009/07/03 08:59:58 $ by $Author: valdas $
 */
public interface RolesManager {

	public abstract void createIdentitiesForRolesNames(Set<String> rolesNames,
	        Identity identity, long processInstanceId);

	/**
	 * convenience method for createIdentitiesForRolesNames
	 * @param roles
	 * @param identity
	 * @param processInstanceId
	 */
	public abstract void createIdentitiesForRoles(Collection<Role> roles,
	        Identity identity, long processInstanceId);

	public abstract void removeIdentitiesForRoles(Collection<Role> roles,
	        Identity identity, long processInstanceId);

	public abstract void hasRightsToStartTask(long taskInstanceId, int userId)
	        throws BPMAccessControlException;

	public abstract void hasRightsToAssignTask(long taskInstanceId, int userId)
	        throws BPMAccessControlException;

	public abstract void checkPermission(Permission permission)
	        throws BPMAccessControlException;

	public abstract Collection<User> getAllUsersForRoles(
	        Collection<String> rolesNames, long piId);

	public abstract Collection<User> getAllUsersForRoles(
	        Collection<String> rolesNames, long piId, BPMTypedPermission perm);

	public abstract void createTaskRolesPermissions(JbpmContext context, Long taskId, List<Role> roles);

	/**
	 * creates actors for process roles provided, for given process instance
	 *
	 * @param roles
	 * @param processInstance
	 * @return actors created
	 */
	public abstract List<Actor> createProcessActors(Collection<Role> roles, ProcessInstance processInstance);
	public List<Actor> createProcessActors(JbpmContext context, Collection<Role> roles, final ProcessInstance processInstance);

	public abstract void assignTaskRolesPermissions(JbpmContext context, Long taskId, List<Role> roles, Long processInstanceId);

	public abstract void assignRolesPermissions(List<Role> roles,
	        ProcessInstance processInstance);

	public abstract List<Actor> getPermissionsByProcessInstanceId(
	        Long processInstanceId, String processRoleName);

	public abstract List<Actor> getProcessRoles(Collection<String> rolesNames,
	        Long processInstanceId);

	/**
	 * creates or updates task instance scope permissions for role.
	 *
	 * @param role
	 *            - role object, containing role name, and accesses to set
	 * @param taskInstanceId
	 * @param setSameForAttachments
	 *            - set the same access rights for binary variables of the task instance
	 * @param variableName
	 *            - if provided, set rights for variable for that task instance. This is usually
	 *            used for task attachments.
	 */
	public abstract void setTaskRolePermissionsTIScope(Role role,
	        Long taskInstanceId, boolean setSameForAttachments,
	        String variableName);

	public abstract void setContactsPermission(Role role,
	        Long processInstanceId, Integer userId);

	public abstract void setAttachmentPermission(Role role, Long processInstanceId, Long taskInstanceId, String variableIdentifier, Integer userId);

	public abstract List<Role> getRolesPermissionsForTaskInstance(
	        Long taskInstanceId, String variableName);

	public abstract Collection<Role> getUserPermissionsForRolesContacts(
	        Long processInstanceId, Integer userId);

	public abstract List<Long> getProcessInstancesIdsForUser(IWContext iwc,
	        User user, boolean checkIfSuperAdmin);

	public abstract List<String> getRolesForAccess(long processInstanceId,
	        Access access);

	public abstract boolean checkFallsInRole(String roleName,
	        List<NativeIdentityBind> nativeIdentities, int userId,
	        AccessController ac, IWApplicationContext iwac);

	public abstract List<Role> getUserRoles(long processInstanceId, User user);

	public abstract PermissionsFactory getPermissionsFactory();

	/**
	 * @param user
	 * @return all idega roles (ones set for user's groups) user is member of
	 */
	public abstract Set<String> getUserNativeRoles(User user);

	public abstract User getUser(int userId);

	public abstract void createIdentitiesForActors(Collection<Actor> actors,
	        Identity identity, long processInstanceId);

	public abstract void removeIdentitiesForActors(Collection<Actor> actors,
	        Identity identity, long processInstanceId);

	public abstract void setTaskPermissionsTIScopeForActors(
	        final List<Actor> actorsToSetPermissionsTo,
	        final List<Access> accesses, final Long taskInstanceId,
	        final boolean setSameForAttachments, final String variableIdentifier);

	/**
	 * assigns roles identities taken from the identities property
	 *
	 * @param processInstance
	 * @param roles
	 */
	public abstract void assignIdentities(ProcessInstance processInstance,
	        List<Role> roles);

	public abstract boolean canSeeComments(Long processInstanceId, User user);
	public abstract boolean canWriteComments(Long processInstanceId, User user);

	public abstract boolean doDisableAttachmentForAllRoles(Integer fileHash, Long processInstanceId, Long taskInstanceId);
}