package com.idega.jbpm.identity.authorization;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.List;
import java.util.Set;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.data.ProcessRoleNativeIdentityBind;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/03/07 16:18:19 $ by $Author: civilis $
 */
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BpmBindsDAO bpmBindsDAO;

	public void checkPermission(Permission perm) throws AccessControlException {

		if(!(perm instanceof BPMTaskAccessPermission))
			throw new IllegalArgumentException("Only permissions implementing "+BPMTaskAccessPermission.class.getName()+" supported");
		
		BPMTaskAccessPermission permission = (BPMTaskAccessPermission)perm;
		
		String loggedInActorId = getAuthenticationService().getActorId();

		TaskInstance taskInstance = permission.getTaskInstance();
		
		if(taskInstance.getActorId() != null) {
			
			if(!loggedInActorId.equals(taskInstance))
				throw new AccessControlException("You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "+taskInstance.getActorId()+", taskInstanceId: "+taskInstance.getId());
			
		} else {

			@SuppressWarnings("unchecked")
			Set<String> pooledActors = taskInstance.getPooledActors();
			
			if(!pooledActors.isEmpty()) {
				throw new AccessControlException("You shall not pass. Pooled actors set was empty, for taskInstanceId: "+taskInstance.getId());
				
			} else {

				checkPermissionInPooledActors(loggedInActorId, pooledActors, !taskInstance.hasEnded());
			}
		}
	}
	
	protected void checkPermissionInPooledActors(String actorId, Set<String> pooledActors, boolean write) throws AccessControlException {
	
		List<ProcessRoleNativeIdentityBind> assignedRolesIdentities = getBpmBindsDAO().getAllProcessRoleNativeIdentityBindsByActors(pooledActors);
		
		for (ProcessRoleNativeIdentityBind assignedRoleIdentity : assignedRolesIdentities) {
			
			System.out.println("assignedRoleIdentity: "+assignedRoleIdentity);
		}
	}

	public void close() { }

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
}