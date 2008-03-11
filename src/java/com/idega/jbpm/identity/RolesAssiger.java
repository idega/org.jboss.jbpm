package com.idega.jbpm.identity;

import java.util.ArrayList;
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
 * @version $Revision: 1.5 $
 * 
 * Last modified: $Date: 2008/03/11 20:14:26 $ by $Author: civilis $
 */
public class RolesAssiger {
	
	private BPMDAO bpmBindsDAO;
	private RolesManager rolesManager;

	public void assign(TaskInstance taskInstance, List<Role> roles) {
		
		if(roles == null || roles.isEmpty()) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No roles for task instance: "+taskInstance.getId());
			return;
		}
		
		HashMap<String, Role> rolz = new HashMap<String, Role>(roles.size());
		
		for (Role role : roles)
			rolz.put(role.getRoleName(), role);
		
		List<ProcessRole> processRoles = getRolesManager().createRolesByProcessInstance(rolz, taskInstance.getProcessInstance().getId());
		getRolesManager().assignTaskAccesses(taskInstance.getId(), processRoles, rolz);
		
//		TODO: assign identity to specific roles (owner)
		
		setPooledActors(taskInstance, processRoles);
	}
	
	private void setPooledActors(TaskInstance taskInstance, List<ProcessRole> processRoles) {
		
		ArrayList<String> actorIds = new ArrayList<String>(processRoles.size());
		
		for (ProcessRole role : processRoles)
			actorIds.add(role.getActorId().toString());
		
		taskInstance.setPooledActors(actorIds.toArray(new String[] {}));
	}
	
	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}