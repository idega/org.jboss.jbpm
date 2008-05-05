package com.idega.jbpm.identity.authorization;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
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

import com.google.common.base.Join;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.data.IDORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $
 *
 * Last modified: $Date: 2008/05/05 16:28:10 $ by $Author: laddi $
 */
@Scope("singleton")
@Service
@Transactional(readOnly=true)
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BPMDAO bpmBindsDAO;

	public void checkPermission(Permission perm) throws AccessControlException {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		
//		faces context == null when permission is called by the system (i.e. not the result of user navigation)
		if(fctx == null)
			return;
Join.join(":", "a", "b", "c");
		if(!(perm instanceof BPMTaskAccessPermission))
			throw new IllegalArgumentException("Only permissions implementing "+BPMTaskAccessPermission.class.getName()+" supported");
		
		BPMTaskAccessPermission permission = (BPMTaskAccessPermission)perm;
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		TaskInstance taskInstance = permission.getTaskInstance();
		
		if(loggedInActorId.equals(taskInstance.getActorId()))
			return;
		
		if(taskInstance.getActorId() != null && !permission.getCheckOnlyInActorsPool()) {
			
			if(!loggedInActorId.equals(taskInstance.getActorId()))
				throw new AccessControlException("You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "+taskInstance.getActorId()+", taskInstanceId: "+taskInstance.getId());
			
		} else {
			
//			super admin always gets an access
			if(IWContext.getIWContext(fctx).isSuperAdmin())
				return;

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
	
	/*
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
	*/
	
	/*
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
	*/
	
	protected boolean fallsInGroups(int userId, List<NativeIdentityBind> nativeIdentities) {
	
		try {
			UserBusiness ub = getUserBusiness();
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
	
	protected boolean fallsInUsers(int userId, List<NativeIdentityBind> nativeIdentities) {
		
		for (NativeIdentityBind nativeIdentity : nativeIdentities) {
			
			if(nativeIdentity.getIdentityType() == IdentityType.USER && nativeIdentity.getIdentityId().equals(String.valueOf(userId)))
				return true;
		}
		
		return false;
	}
	
	protected void checkPermissionInPooledActors(int userId, Collection<Long> pooledActors, TaskInstance taskInstance) throws AccessControlException {
		
		List<ProcessRole> roles = getBpmBindsDAO().getProcessRoles(pooledActors);
		
		boolean writeAccessNeeded = !taskInstance.hasEnded();
		
		try {
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			UserBusiness userBusiness = getUserBusiness();
			
			Collection<Group> usrGrps = userBusiness.getUserGroups(userId);
			long taskId = taskInstance.getTask().getId();
			
			for (ProcessRole processRole : roles) {
				
				List<ActorPermissions> perms = processRole.getActorPermissions();
				
				if(perms != null) {
					
					for (ActorPermissions perm : perms) {
						
						if(perm.getReadPermission() && (!writeAccessNeeded || perm.getWritePermission()) &&
								((perm.getTaskInstanceId() != null && perm.getTaskInstanceId().equals(taskInstance.getId())) || (perm.getTaskInstanceId() == null && perm.getTaskId().equals(taskId)))) {
							
							if(processRole.getProcessInstanceId() == null) {
								
								String roleName = processRole.getProcessRoleName();
								
								Collection<Group> grps = getAccessController().getAllGroupsForRoleKey(roleName, iwac);
								
								for (Group roleGrp : grps) {
									
									if(usrGrps.contains(roleGrp)) {
										
//										falls in group satisfying permission
										return;
									}
								}
								
							} else {
								
								List<NativeIdentityBind> nativeIdentities = processRole.getNativeIdentities();
						
								if(fallsInUsers(userId, nativeIdentities) || fallsInGroups(userId, nativeIdentities))
									return;
							}
						}
					}
				}
			}
			
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
		
		throw new AccessControlException("User ("+userId+") doesn't fall into any of pooled actors ("+pooledActors+").");
	
/*
		List<NativeIdentityBind> nativeIdentities = resolveCandidates(pooledActors, taskInstance);
		
		if(!nativeIdentities.isEmpty()) {
			
//			check for users
			if(fallsInUsers(userId, nativeIdentities))
				return;

//			check for groups:
			if(fallsInGroups(userId, nativeIdentities))
				return;
			
//			check for roles
		}
*/		
	}
	
/*
	protected boolean fallsIn(int userId, List<NativeIdentityBind> nativeIdentities) {
		
		for (NativeIdentityBind nativeIdentity : nativeIdentities) {
			
			if(nativeIdentity.getIdentityType() == IdentityType.USER && nativeIdentity.getIdentityId().equals(String.valueOf(userId)))
				return true;
		}
		
		return false;
	}
	*/
	
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
	
	protected AccessController getAccessController() {
		
		return getIWMA().getAccessController();
	}
	
	protected IWMainApplication getIWMA() {
		
		IWMainApplication iwma;
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		if(fctx == null)
			iwma = IWMainApplication.getDefaultIWMainApplication();
		else
			iwma = IWMainApplication.getIWMainApplication(fctx);
		
		return iwma;
	}
}