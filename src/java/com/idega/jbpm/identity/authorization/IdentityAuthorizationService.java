package com.idega.jbpm.identity.authorization;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.data.IDORuntimeException;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 * Last modified: $Date: 2008/03/13 20:13:14 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
@Transactional(readOnly=true)
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BPMDAO bpmBindsDAO;

	public void checkPermission(Permission perm) throws AccessControlException {

		if(!(perm instanceof BPMTaskAccessPermission))
			throw new IllegalArgumentException("Only permissions implementing "+BPMTaskAccessPermission.class.getName()+" supported");
		
		BPMTaskAccessPermission permission = (BPMTaskAccessPermission)perm;
		
		String loggedInActorId = getAuthenticationService().getActorId();

		TaskInstance taskInstance = permission.getTaskInstance();
		
		if(taskInstance.getActorId() != null) {
			
			if(!loggedInActorId.equals(taskInstance.getActorId()))
				throw new AccessControlException("You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "+taskInstance.getActorId()+", taskInstanceId: "+taskInstance.getId());
			
		} else {
			
//			super admin always gets an access
			if(IWContext.getIWContext(FacesContext.getCurrentInstance()).isSuperAdmin())
				return;

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
				checkPermissionInPooledActors(new Integer(loggedInActorId), pooledActorsIds, taskInstance);
			}
		}
	}
	
	protected List<NativeIdentityBind> resolveCandidates(Collection<Long> pooledActors, TaskInstance taskInstance) {
		
		List<ProcessRole> roles = getBpmBindsDAO().getProcessRoles(pooledActors);
		HashSet<String> genRoleNamesToCheck = new HashSet<String>(roles.size());
		
		ArrayList<NativeIdentityBind> candidates = new ArrayList<NativeIdentityBind>();
		
//		first check amongst assigned to task instance (bound to process instance)
		for (ProcessRole processRole : roles) {
			
			if(isRoleSatisfiesPermission(processRole, taskInstance)) {
			
				if(processRole.getNativeIdentities().isEmpty()) {
					
					genRoleNamesToCheck.add(processRole.getProcessRoleName());
					
				} else {
					
					candidates.addAll(processRole.getNativeIdentities());
				}
			}
		}
		
//		checking amongst general roles
		if(!genRoleNamesToCheck.isEmpty()) {
			
			roles = getBpmBindsDAO().getProcessRolesByRolesNames(genRoleNamesToCheck, null);
			
			for (ProcessRole processRole : roles) {

				candidates.addAll(processRole.getNativeIdentities());
			}
		}
		
		return candidates;
	}
	
	protected boolean isRoleSatisfiesPermission(ProcessRole role, TaskInstance taskInstance) {
		
		String jsonExp = taskInstance.getTask().getAssignmentDelegation().getConfiguration();
		
		List<Role> roles = JSONExpHandler.resolveRolesFromJSONExpression(jsonExp);
		boolean writeAccessNeeded = !taskInstance.hasEnded();
		
		for (Role confRole : roles) {

			if(confRole.getRoleName().equals(role.getProcessRoleName())) {
				
				if(confRole.getAccesses().contains(Access.read) && (!writeAccessNeeded || confRole.getAccesses().contains(Access.write)))
					return true;
				else
					return false;
			}
		}
		
		return false;
	}
	
	protected boolean fallsInGroups(int userId, List<NativeIdentityBind> nativeIdentities) {
	
		try {
			UserBusiness ub = getUserBusiness();
			@SuppressWarnings("unchecked")
			Collection<Group> userGroups = ub.getUserGroups(userId);
			
			for (Group group : userGroups) {
			
				String groupId = group.getPrimaryKey().toString();
				
				for (NativeIdentityBind nativeIdentity : nativeIdentities) {
					
					if(nativeIdentity.getIdentityType() == IdentityType.GROUP && nativeIdentity.getIdentityId().equals(groupId))
						return true;
				}
			}
			
			return false;
			
		} catch (RemoteException e) {
			throw new IDORuntimeException(e);
		}
	}
	
	protected void checkPermissionInPooledActors(int userId, Collection<Long> pooledActors, TaskInstance taskInstance) throws AccessControlException {
	

		List<NativeIdentityBind> nativeIdentities = resolveCandidates(pooledActors, taskInstance);

//		check for groups:
		if(!nativeIdentities.isEmpty()) {
			
			if(fallsInGroups(userId, nativeIdentities))
				return;
		}
		
//		check for roles
//		check for users
	
		throw new AccessControlException("User ("+userId+") doesn't fall into any of pooled actors ("+pooledActors+").");
		
		/*
		if(true)
			return;
		
		List<ProcessRole> assignedRolesIdentities = getBpmBindsDAO().getProcessRoles(pooledActors, taskInstance.getId());
		ArrayList<Long> filteredRolesIdentities = new ArrayList<Long>(assignedRolesIdentities.size());
		
		boolean writeAccessNeeded = !taskInstance.hasEnded();
		
		for (ProcessRole assignedRoleIdentity : assignedRolesIdentities) {

			List<TaskInstanceAccess> accesses = assignedRoleIdentity.getTaskInstanceAccesses();
			
			for (TaskInstanceAccess tiAccess : accesses) {
				
				if(tiAccess.getTaskInstanceId() == taskInstance.getId()) {
			
					if(tiAccess.hasAccess(Access.read) && (!writeAccessNeeded || tiAccess.hasAccess(Access.write))) {
				
						filteredRolesIdentities.add(assignedRoleIdentity.getActorId());
					}
					
					break;
				}
			}
		}
		
		if(!filteredRolesIdentities.isEmpty()) {

//			check for groups:
			List<NativeIdentityBind> nativeIdentities = getBpmBindsDAO().getNativeIdentities(filteredRolesIdentities, IdentityType.GROUP);
			
			if(!nativeIdentities.isEmpty()) {
				
				try {
					UserBusiness ub = getUserBusiness();
					@SuppressWarnings("unchecked")
					Collection<Group> userGroups = ub.getUserGroups(userId);
					
					for (Group group : userGroups) {
					
						String groupId = group.getPrimaryKey().toString();
						
						for (NativeIdentityBind nativeIdentity : nativeIdentities) {
							
							if(nativeIdentity.getIdentityId().equals(groupId))
								return;
						}
					}
					
				} catch (RemoteException e) {
					throw new IDORuntimeException(e);
				}
			}

//			check for roles
//			check for users
		}
		
		throw new AccessControlException("User ("+userId+") doesn't fall into any of pooled actors ("+pooledActors+"). Write access needed: "+writeAccessNeeded);
		*/
	}
	
	public void close() { }

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	@Autowired
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}