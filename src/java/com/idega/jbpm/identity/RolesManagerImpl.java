package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.dao.BPMDAO;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/03/12 11:43:54 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmRolesManager")
public class RolesManagerImpl implements RolesManager {
	
	private BPMDAO bpmDAO;

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
	
	public void assignTaskAccesses(long taskInstanceId, List<ProcessRole> processRoles, Map<String, Role> roles) {
		
		HashMap<Role, ProcessRole> proles = new HashMap<Role, ProcessRole>(processRoles.size());
		
		for (ProcessRole processRole : processRoles) {
			
			proles.put(roles.get(processRole.getProcessRoleName()), processRole);
		}
		
		getBpmDAO().updateAssignTaskAccesses(taskInstanceId, proles);
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
}