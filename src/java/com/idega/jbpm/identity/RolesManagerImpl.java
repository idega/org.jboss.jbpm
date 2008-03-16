package com.idega.jbpm.identity;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
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
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 * 
 * Last modified: $Date: 2008/03/16 19:00:30 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmRolesManager")
public class RolesManagerImpl implements RolesManager {
	
	private BPMDAO bpmDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private AuthorizationService authorizationService;

	public List<ProcessRole> createRolesByProcessInstance(Map<String, Role> roles, long processInstanceId) {
		
		List<ProcessRole> processRoles = findAndCreateProcessRoles(roles.keySet(), processInstanceId);
		
		HashSet<String> genRolesToAdd = new HashSet<String>(roles.size());
		
		for (Role role : roles.values()) {
			
			if(role.isGeneral())
				genRolesToAdd.add(role.getRoleName());
		}
		
//		adding general process roles if needed
		findAndCreateProcessRoles(genRolesToAdd, null);
		
		return processRoles;
	}
	
	@Transactional(readOnly=false)
	public void createIdentitiesForRoles(List<ProcessRole> processRoles, int userId) {

		for (ProcessRole role : processRoles) {
			
			if(!Role.isGeneral(role.getProcessRoleName())) {
			
				NativeIdentityBind nidentity = new NativeIdentityBind();
				nidentity.setIdentityId(String.valueOf(userId));
				nidentity.setIdentityType(IdentityType.USER);
				nidentity.setProcessRole((ProcessRole)getBpmDAO().merge(role));
				getBpmDAO().persist(nidentity);
			}
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
			
			if(Role.isGeneral(processRole.getProcessRoleName()))
				genRoles.add(processRole);
		}
		
		return genRoles;
	}
	
//	TODO: check if noone else tries to create roles for this piId at the same time
	protected List<ProcessRole> findAndCreateProcessRoles(Set<String> rolesNames, Long processInstanceId) {
		
		List<ProcessRole> processRoles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		Set<String> rolesToCreate = getNonExistentRoles(rolesNames, processRoles);
		
		if(!rolesToCreate.isEmpty()) {
			getBpmDAO().updateCreateProcessRoles(rolesToCreate, processInstanceId);
			processRoles = getBpmDAO().getProcessRolesByRolesNames(rolesNames, processInstanceId);
		}
		
		return processRoles;
	}
	
	private Set<String> getNonExistentRoles(Set<String> rolesNames, List<ProcessRole> processRoles) {
		
		if(processRoles.isEmpty())
			return rolesNames;
		
		HashSet<String> rolesToCreate = new HashSet<String>(rolesNames.size());
		
		for (String roleName : rolesNames) {
		
			boolean takeIt = true;
			
			for (ProcessRole role : processRoles) {
				
				if(role.getProcessRoleName().equals(roleName)) {
					takeIt = false;
					break;
				}
			}
			
			if(takeIt)
				rolesToCreate.add(roleName);
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
	
	public void hasRightsToAsssignTask(long taskInstanceId, int userId) throws BPMAccessControlException {
		
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