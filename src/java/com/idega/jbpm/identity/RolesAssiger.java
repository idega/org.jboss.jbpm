package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.dao.BPMDAO;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/03/11 12:16:58 $ by $Author: civilis $
 */
public class RolesAssiger {
	
	private BPMDAO bpmBindsDAO;

	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public void assign(TaskInstance taskInstance, List<Role> roles) {
		
		if(roles == null || roles.isEmpty()) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No roles for task instance: "+taskInstance.getId());
			return;
		}
		
		HashMap<String, Role> rolz = new HashMap<String, Role>(roles.size());
		
		for (Role role : roles) {
			
			rolz.put(role.getRoleName(), role);
		}
		
		List<ProcessRole> processRoles = getBpmBindsDAO().getAllProcessRoleNativeIdentityBinds(rolz.keySet());
		
		Collection<String> actorIds;
		Long processInstanceId = taskInstance.getProcessInstance().getId();
		
		if(processRoles == null || processRoles.isEmpty()) {
			
			actorIds = getBpmBindsDAO().updateCreatePRolesAndAssignTaskAccesses(taskInstance, roles);
			
		} else {
			
			HashMap<Role, ProcessRole> proles = new HashMap<Role, ProcessRole>(processRoles.size());
			
			for (ProcessRole processRole : processRoles) {
				
				if(processInstanceId.equals(processRole.getProcessInstanceId()) || !proles.containsKey(processRole.getProcessRoleName()))
					proles.put(rolz.get(processRole.getProcessRoleName()), processRole);
			}
			
			actorIds = getBpmBindsDAO().updateAssignTaskAccesses(taskInstance.getId(), proles);
			
			if(processRoles.size() < roles.size()) {
				
//				There are some roles that are not put in ProcessRoles table
				
				ArrayList<Role> rolesToCreate = new ArrayList<Role>(roles.size() - processRoles.size());
				
				for (Role role : roles) {
					
					boolean takeIt = true;
					
					for (ProcessRole prole : processRoles) {
						
						if(role.getRoleName().equals(prole.getProcessRoleName())) {
							takeIt = false;
							break;
						}
					}
					
					if(takeIt)
						rolesToCreate.add(role);
				}
				
				Collection<String> actorIds2 = getBpmBindsDAO().updateCreatePRolesAndAssignTaskAccesses(taskInstance, rolesToCreate);
				actorIds.addAll(actorIds2);
			}
		}
		
		taskInstance.setPooledActors(actorIds.toArray(new String[] {}));
	}
}