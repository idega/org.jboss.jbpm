package com.idega.jbpm.identity;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.JbpmContext;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMAccessControlException;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $
 * 
 * Last modified: $Date: 2008/04/17 01:16:41 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmRolesManager")
public class RolesManagerImpl implements RolesManager {
	
	private BPMDAO bpmDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private AuthorizationService authorizationService;

	public List<ProcessRole> createRolesByProcessInstance(Map<String, Role> roles, long processInstanceId) {
		
		if(roles.isEmpty())
			return new ArrayList<ProcessRole>(0);
		
		List<ProcessRole> processRoles = findAndCreateProcessRoles(new HashSet<Role>(roles.values()), processInstanceId);
		
		HashSet<Role> genRolesToAdd = new HashSet<Role>(roles.size());
		
		for (Role role : roles.values()) {
			
			if(role.getScope() == RoleScope.PD)
				genRolesToAdd.add(role);
		}
		
		if(!genRolesToAdd.isEmpty()) {

//			adding general process roles if needed
			findAndCreateProcessRoles(genRolesToAdd, null);
		}
		
		return processRoles;
	}
	
	@Transactional(readOnly=false)
	public void createIdentitiesForRoles(Collection<Role> roles, String identityId, IdentityType identityType, long processInstanceId) {
		
		if(roles.isEmpty())
			return;

		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles)
			rolesNames.add(role.getRoleName());
		
		List<ProcessRole> processRoles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		
		for (ProcessRole role : processRoles) {
			
			List<NativeIdentityBind> identities = role.getNativeIdentities();
			
			boolean contains = false;
			
			for (NativeIdentityBind nativeIdentityBind : identities) {
				
				if(identityType == nativeIdentityBind.getIdentityType() && identityId.equals(nativeIdentityBind.getIdentityId())) {
					contains = true;
					break;
				}
			}
			
			if(contains)
				continue;
			
			NativeIdentityBind nidentity = new NativeIdentityBind();
			nidentity.setIdentityId(identityId);
			nidentity.setIdentityType(identityType);
			nidentity.setProcessRole((ProcessRole)getBpmDAO().merge(role));
			getBpmDAO().persist(nidentity);
		}
	}
	
	public void addGroupsToRoles(Long actorId, Collection<String> groupsIds, Long processInstanceId, Long processDefinitionId) {
		
		if(processInstanceId == null && processDefinitionId == null) {
		
			getBpmDAO().updateAddGrpsToRole(actorId, groupsIds);
		} else
			throw new UnsupportedOperationException("processInstanceId: "+processInstanceId+", processDefinitionId: "+processDefinitionId+". Assignment for general roles implemented only");
	}
	
	public List<ProcessRole> getGeneralRoles() {
		
		List<ProcessRole> roles = getBpmDAO().getAllGeneralProcessRoles();
		ArrayList<ProcessRole> genRoles = new ArrayList<ProcessRole>(roles.size());
		
		for (ProcessRole processRole : roles) {
			
			if(processRole.getRoleScope() != RoleScope.PI)
				genRoles.add(processRole);
		}
		
		return genRoles;
	}
	
//	TODO: check if noone else tries to create roles for this piId at the same time
	protected List<ProcessRole> findAndCreateProcessRoles(Set<Role> roles, Long processInstanceId) {
		
		HashSet<String> rolesNames = new HashSet<String>(roles.size());
		
		for (Role role : roles)
			rolesNames.add(role.getRoleName());
		
		List<ProcessRole> processRoles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		Set<Role> rolesToCreate = getNonExistentRoles(roles, processRoles);
		
		if(!rolesToCreate.isEmpty()) {
			getBpmDAO().updateCreateProcessRoles(rolesToCreate, processInstanceId);
			processRoles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		}
		
		return processRoles;
	}
	
	private Set<Role> getNonExistentRoles(Set<Role> roles, List<ProcessRole> processRoles) {
		
		if(processRoles.isEmpty())
			return roles;
		
		HashSet<Role> rolesToCreate = new HashSet<Role>(roles.size());
		
		for (Role role : roles) {
		
			boolean takeIt = true;
			
			for (ProcessRole prole : processRoles) {
				
				if(prole.getProcessRoleName().equals(role.getRoleName())) {
					takeIt = false;
					break;
				}
			}
			
			if(takeIt)
				rolesToCreate.add(role);
		}
		
		return rolesToCreate;
	}
	
	public BPMDAO getBpmDAO() {
		return bpmDAO;
	}

	@Autowired
	public void setBpmDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}
	
	public void hasRightsToStartTask(long taskInstanceId, int userId) throws BPMAccessControlException {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.getStart() != null || taskInstance.hasEnded())
				throw new BPMAccessControlException("Task ("+taskInstanceId+") has already been started, or has already ended", "Task has already been started, or has already ended");
			
			if(taskInstance.getActorId() == null || !taskInstance.getActorId().equals(String.valueOf(userId)))
				throw new BPMAccessControlException("User ("+userId+") tried to start task, but not assigned to the user provided. Assigned: "+taskInstance.getActorId(), "User should be taken or assigned of the task first to start working on it");
			
			SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			getAuthorizationService().checkPermission(permission);
		
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("User has no access to modify this task");
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public void checkPermission(Permission permission) throws BPMAccessControlException {
		
		try {
			getAuthorizationService().checkPermission(permission);
			
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("No access");
		}
	}
	
	public void hasRightsToAssignTask(long taskInstanceId, int userId) throws BPMAccessControlException {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.getStart() != null || taskInstance.hasEnded())
				throw new BPMAccessControlException("Task ("+taskInstanceId+") has already been started, or has already ended", "Task has been started by someone, or has ended.");
			
			if(taskInstance.getActorId() != null)
				throw new BPMAccessControlException("Task ("+taskInstanceId+") is already assigned to: "+taskInstance.getActorId(), "This task has been assigned already");
			
			SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			getAuthorizationService().checkPermission(permission);
		
		} catch (BPMAccessControlException e) {
			throw e;
		} catch (AccessControlException e) {
			throw new BPMAccessControlException("User has no access to modify this task");
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	@Transactional(readOnly=true)
	public Map<String, List<NativeIdentityBind>> getIdentitiesForRoles(Collection<String> rolesNames, long processInstanceId) {

		List<ProcessRole> roles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		
		HashMap<String, List<NativeIdentityBind>> identities = new HashMap<String, List<NativeIdentityBind>>(roles.size());
		ArrayList<String> rolesNamesToCheckForGeneral = new ArrayList<String>(roles.size());
		
		for (ProcessRole processRole : roles) {
			
			if(!processRole.getNativeIdentities().isEmpty()) {
				identities.put(processRole.getProcessRoleName(), processRole.getNativeIdentities());
			} else {
				rolesNamesToCheckForGeneral.add(processRole.getProcessRoleName());
			}
		}
		
		if(!rolesNamesToCheckForGeneral.isEmpty()) {
		
			roles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, null);
			
			for (ProcessRole processRole : roles) {
				
				if(!processRole.getNativeIdentities().isEmpty())
					identities.put(processRole.getProcessRoleName(), processRole.getNativeIdentities());
			}
		}
		
		return identities;
	}
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Autowired
	public void setAuthorizationService(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}
}