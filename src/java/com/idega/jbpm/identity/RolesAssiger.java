package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $
 * 
 * Last modified: $Date: 2008/05/24 10:25:51 $ by $Author: civilis $
 */
public class RolesAssiger {
	
	private static final String ASSIGN_IDENTITY_CURRENT_USER = "current_user";
	
	private BPMDAO bpmBindsDAO;
	private RolesManager rolesManager;
	private BPMFactory bpmFactory;

	public void assign(TaskInstance taskInstance, List<Role> roles) {
		
		if(roles == null || roles.isEmpty()) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No roles for task instance: "+taskInstance.getId());
			return;
		}
		
		ProcessInstance pi = taskInstance.getProcessInstance();
		
		List<ProcessRole> processRoles = getRolesManager().createProcessRoles(pi.getProcessDefinition().getName(), roles, pi.getId());
		setPooledActors(taskInstance, processRoles);
	}
	
	public void createRolesPermissions(TaskInstance taskInstance, List<Role> roles) {
	
		if(roles == null || roles.isEmpty()) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No roles for task instance: "+taskInstance.getId());
			return;
		}
		
		ProcessInstance pi = taskInstance.getProcessInstance();
		
		getRolesManager().assignTaskRolesPermissions(taskInstance.getTask(), roles, pi.getId());
	}
	
	public void assignIdentities(TaskInstance taskInstance, List<Role> roles) {
		
		if(roles == null || roles.isEmpty()) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No roles for task instance: "+taskInstance.getId());
			return;
		}
		
		/*
		FacesContext fctx = FacesContext.getCurrentInstance();
		if(fctx == null)
			return;
		
//		currently supporting only assigning to current user
		Integer userId = IWContext.getIWContext(fctx).getCurrentUserId();
		*/
		
		Integer usrId = getBpmFactory().getBpmUserFactory().getCurrentBPMUser().getIdToUse();
		
		if(usrId != null) {
		
			ArrayList<Role> rolesToAssignIdentity = new ArrayList<Role>(roles.size());
			
			for (Role role : roles) {
				
				if(role.getAssignIdentities() != null) {
					
					for (String assignTo : role.getAssignIdentities()) {
						
						if(assignTo.equals(ASSIGN_IDENTITY_CURRENT_USER)) {
							rolesToAssignIdentity.add(role);
							break;
						}
					}
				}
			}
			
			if(!rolesToAssignIdentity.isEmpty()) {
				
				getRolesManager().createIdentitiesForRoles(rolesToAssignIdentity, String.valueOf(usrId), IdentityType.USER, taskInstance.getProcessInstance().getId());
			}
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "assignIdentities called, but no user id resolved from current bpm user");
		}
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

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}