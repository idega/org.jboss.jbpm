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
 * @version $Revision: 1.14 $
 * 
 * Last modified: $Date: 2008/05/23 08:19:55 $ by $Author: civilis $
 */
public interface RolesManager {

//	public abstract List<ProcessRole> createRolesByProcessInstance(
//			Map<String, Role> roles, long processInstanceId);
	
//	public abstract void createTaskRolesPermissionsPIScope(Task task, List<Role> roles, Long processInstanceId);
//	
//	public abstract void createTaskRolesPermissionsPDScope(Task task, List<Role> roles);
	
	//public abstract void addGroupsToRoles(Long actorId, Collection<String> groupsIds, Long processInstanceId, Long processDefinitionId);
	
	public abstract void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId);
	
	public abstract void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void hasRightsToAssignTask(long taskInstanceId, int userId) throws BPMAccessControlException;
	
	public abstract void checkPermission(Permission permission) throws BPMAccessControlException;
	
	//public abstract Map<String, List<NativeIdentityBind>> getIdentitiesForRoles(Collection<String> rolesNames, long processInstanceId);
	public abstract Collection<User> getAllUsersForRoles(Collection<String> rolesNames, ProcessInstance pi);
	
	public abstract void createNativeRolesFromProcessRoles(String processName, Collection<Role> roles);
	
	public abstract void createTaskRolesPermissions(Task task, List<Role> roles);
	
//	public abstract List<ProcessRole> createProcessRolesForPDScope(String processName, Collection<Role> roles, Long processInstanceId);
//	
//	public abstract List<ProcessRole> createProcessRolesForPIScope(String processName, Collection<Role> roles, Long processInstanceId);
	
	public abstract List<ProcessRole> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId);
	
	public abstract void assignTaskRolesPermissions(Task task, List<Role> roles, Long processInstanceId);

	public abstract List<ProcessRole> getProcessRolesForProcessInstanceByTaskInstance(Long taskInstanceId);
	
	//public abstract List<ProcessRole> createProcessRoles(String processName, Collection<Role> roles, Long processInstanceId);
	
	public abstract List<Long> getProcessInstancesIdsForCurrentUser();
}