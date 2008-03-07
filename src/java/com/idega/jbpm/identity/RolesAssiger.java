package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.taskmgmt.exe.Assignable;

import com.idega.jbpm.data.ProcessRoleNativeIdentityBind;
import com.idega.jbpm.data.dao.BpmBindsDAO;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/03/07 13:26:40 $ by $Author: civilis $
 */
public class RolesAssiger {
	
	private BpmBindsDAO bpmBindsDAO;

	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public void assign(Assignable assignable, List<Role> roles) {
		
		ArrayList<String> rolesNames = new ArrayList<String>(roles.size());
		
		for (Role role : roles) {
			
			rolesNames.add(role.getRoleName());
		}
		
		List<ProcessRoleNativeIdentityBind> rolesIdentities = getBpmBindsDAO().getAllProcessRoleNativeIdentityBinds(rolesNames);
		
		if(rolesIdentities == null || rolesIdentities.isEmpty()) {
			
			String defaultGroupName = "transformers";
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Found nothing to assign to for roles: "+roles+". Assigning to default group ("+defaultGroupName+")");
		} else {
		
//			TODO: start caring about read write accesses
//			TODO: check, if for each Role identity exist (match in db), if not, assign to default
			HashSet<String> actorIds = new HashSet<String>(rolesIdentities.size());
			
			for (ProcessRoleNativeIdentityBind roleIdentity : rolesIdentities) {
				
				actorIds.add(roleIdentity.getActorId().toString());
			}
			
			assignable.setPooledActors(actorIds.toArray(new String[] {}));
		}
	}
}