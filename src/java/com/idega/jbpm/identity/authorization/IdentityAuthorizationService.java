package com.idega.jbpm.identity.authorization;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.ProcessRoleNativeIdentityBind;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/03/08 10:02:44 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
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
			Set<PooledActor> pooledActors = taskInstance.getPooledActors();
			
			if(pooledActors.isEmpty()) {
				throw new AccessControlException("You shall not pass. Pooled actors set was empty, for taskInstanceId: "+taskInstance.getId());
				
			} else {

				Collection<Long> pooledActorsIds = new ArrayList<Long>(pooledActors.size());
				
				for (PooledActor pooledActor : pooledActors) {
					
					Long actorId = new Long(pooledActor.getActorId());
					pooledActorsIds.add(actorId);
				}
				checkPermissionInPooledActors(loggedInActorId, pooledActorsIds, !taskInstance.hasEnded());
			}
		}
	}
	
	protected void checkPermissionInPooledActors(String actorId, Collection<Long> pooledActors, boolean write) throws AccessControlException {
	
		List<ProcessRoleNativeIdentityBind> assignedRolesIdentities = getBpmBindsDAO().getAllProcessRoleNativeIdentityBindsByActors(pooledActors);
		
		for (ProcessRoleNativeIdentityBind assignedRoleIdentity : assignedRolesIdentities) {
			
			System.out.println("assignedRoleIdentity: "+assignedRoleIdentity);
		}
	}

	public void close() { }

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	@Autowired
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
}