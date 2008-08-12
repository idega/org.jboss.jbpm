package com.idega.jbpm.identity.authorization;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.permission.BPMTypedHandler;
import com.idega.jbpm.identity.permission.BPMTypedPermission;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.35 $
 *
 * Last modified: $Date: 2008/08/12 10:58:30 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
@Transactional(readOnly=true, noRollbackFor={AccessControlException.class})
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BPMDAO bpmBindsDAO;
	private Map<String, BPMTypedHandler> handlers;
	
	/*
	protected void checkTaskAccessPermission(FacesContext fctx, BPMTaskAccessPermission permission) throws AccessControlException {
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
		TaskInstance taskInstance = permission.getTaskInstance();
		
		if(loggedInActorId.equals(taskInstance.getActorId()))
			return;
		
		if(!taskInstance.hasEnded() && taskInstance.getActorId() != null && !permission.getCheckOnlyInActorsPool()) {
			
			if(!loggedInActorId.equals(taskInstance.getActorId()))
				throw new AccessControlException("You shall not pass. Logged in actor id doesn't match the assigned actor id. Assigned: "+taskInstance.getActorId()+", taskInstanceId: "+taskInstance.getId());
			
		} else {
			
//			super admin always gets an access
			if(!IWContext.getIWContext(fctx).isSuperAdmin()) {
				
				if(permission instanceof BPMTaskVariableAccessPermission) {
				
					BPMTaskVariableAccessPermission vperm = (BPMTaskVariableAccessPermission)permission;
					
					if(vperm.getVariableIndentifier() == null || CoreConstants.EMPTY.equals(vperm.getVariableIndentifier()))
						throw new IllegalArgumentException("Illegal permission passed. Passed of type="+BPMTaskVariableAccessPermission.class.getName()+", but not variable identifier provided");
					
					checkPermissionsForTaskInstance(new Integer(loggedInActorId), taskInstance, permission.getAccesses(), vperm.getVariableIndentifier());
				} else {

					checkPermissionsForTaskInstance(new Integer(loggedInActorId), taskInstance, permission.getAccesses(), null);
				}
			}
		}
	}
	*/
	/*
	protected void checkGenericAccessPermission(FacesContext fctx, BPMGenericAccessPermission permission) throws AccessControlException {
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
		if(IWContext.getIWContext(fctx).isSuperAdmin())
			return;
		
		List<ProcessRole> proles = 
			getBpmBindsDAO().getResultList(ProcessRole.getRolesHavingCaseHandlerRights, ProcessRole.class,
					new Param(ProcessRole.processInstanceIdProperty, permission.getProcessInstanceId())
			);
		
		if(proles != null) {
			
			Integer userId = new Integer(loggedInActorId);
			AccessController ac = getAccessController();
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			
			for (ProcessRole processRole : proles) {
				
				if(checkFallsInRole(processRole.getProcessRoleName(), processRole.getNativeIdentities(), userId, ac, iwac))
					return;
			}
		}
		
		throw new AccessControlException("No "+permission.getAccess()+" access for user="+loggedInActorId);
	}
	*/
	
	/*
	protected void checkRoleAccessPermission(FacesContext fctx, BPMRoleAccessPermission permission) throws AccessControlException {
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
		AccessController ac = getAccessController();
		IWApplicationContext iwac = getIWMA().getIWApplicationContext();
		
		Long processInstanceId = permission.getProcessInstanceId();
		String roleName = permission.getRoleName();
		
		if(permission.isCheckContactsForRole()) {
			
//			super admin always gets an access
			if(!IWContext.getIWContext(fctx).isSuperAdmin()) {
				
				List<ActorPermissions> actPerm =
					
					getBpmBindsDAO().getResultList(
							ActorPermissions.getSetByProcessInstanceIdAndContactPermissionsRolesNames,
							ActorPermissions.class,
							new Param(ProcessRole.processInstanceIdProperty, processInstanceId),
							new Param(ActorPermissions.canSeeContactsOfRoleNameProperty, Arrays.asList(new String[] {roleName, "all"}))
					);
				
				if(actPerm != null) {
					
					int userId = new Integer(loggedInActorId);
					
//					find out what roles can see contacts of the role provided in permission
					
					for (ActorPermissions actorPermission : actPerm) {
						
						List<ProcessRole> proles = actorPermission.getProcessRoles();
						
						for (ProcessRole prole : proles) {

//							and check if current user falls in any of those roles
							if(checkFallsInRole(prole.getProcessRoleName(), prole.getNativeIdentities(), userId, ac, iwac))
								return;
						}
					}
				}
				
				throw new AccessControlException("No contacts for role access for user="+loggedInActorId+", for processInstanceId="+processInstanceId+", for role="+roleName);
			}
			
		} else {
			
//			checking basic falling in role
		
			List<ProcessRole> proles = 
				getBpmBindsDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
						new Param(ProcessRole.processInstanceIdProperty, processInstanceId),
						new Param(ProcessRole.processRoleNameProperty, Arrays.asList(new String[] {roleName}))
				);
			
			if(proles == null || proles.isEmpty())
				throw new AccessControlException("No process role found by role name="+roleName+" and process instance id = "+processInstanceId);
			
			ProcessRole prole = proles.iterator().next();
			
			int userId = new Integer(loggedInActorId);

			List<NativeIdentityBind> nativeIdentities = prole.getNativeIdentities();
			
			if(checkFallsInRole(prole.getProcessRoleName(), nativeIdentities, userId, ac, iwac))
				return;

			throw new AccessControlException("No role access for user="+loggedInActorId+", for processInstanceId="+processInstanceId+", for role="+roleName);
		}
	}
	*/
	/*
	protected void checkRightsMgmtPermission(FacesContext fctx, BPMRightsMgmtPermission permission) throws AccessControlException {
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
//		super admin always gets an access
		if(!IWContext.getIWContext(fctx).isSuperAdmin()) {
		
			Long processInstanceId = permission.getProcessInstanceId();
			
			if(processInstanceId == null)
				throw new RuntimeException("No process instance id found in permission="+permission.getClass().getName());
			
			List<String> roleNames = getBpmBindsDAO().getResultList(ProcessRole.getRoleNameHavingRightsModifyPermissionByPIId, String.class,
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId));
			
			if(roleNames != null && !roleNames.isEmpty()) {
			
				List<ProcessRole> proles = 
					getBpmBindsDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
							new Param(ProcessRole.processInstanceIdProperty, processInstanceId),
							new Param(ProcessRole.processRoleNameProperty, roleNames)
					);
				
				if(proles == null || proles.isEmpty())
					throw new AccessControlException("No process role found by role names="+roleNames.toString() +" and process instance id = "+processInstanceId);
				
				ProcessRole prole = proles.iterator().next();
				
				int userId = new Integer(loggedInActorId);

				AccessController ac = getAccessController();
				IWApplicationContext iwac = getIWMA().getIWApplicationContext();
				
				List<NativeIdentityBind> nativeIdentities = prole.getNativeIdentities();
				
				if(checkFallsInRole(prole.getProcessRoleName(), nativeIdentities, userId, ac, iwac))
					return;
			}
			
			throw new AccessControlException("No rights management permission for user="+loggedInActorId+", for processInstanceId="+processInstanceId);
		}
	}
	*/

	@Autowired
	public void setHandlers(List<BPMTypedHandler> handlers) {
		
		if(handlers != null) {
			
//			double size here, as some handlers might support more than one permission type
			this.handlers = new HashMap<String, BPMTypedHandler>(handlers.size()*2);
			
			for (BPMTypedHandler handler : handlers) {
				
				String[] handledTypes = handler.getHandledTypes();
				
				if(handledTypes != null) {
					
					for (String handledType : handledTypes) {
						
						if(handledType != null && !CoreConstants.EMPTY.equals(handledType))
							this.handlers.put(handledType, handler);
					}
					
				} else
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Typed permissions handler registered, but no supported permissions types returned. Handler class="+handler.getClass().getName());
			}
		}
	}

	public void checkPermission(Permission perm) throws AccessControlException {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		
//		faces context == null when permission is called by the system (i.e. not the result of user navigation)
		if(fctx == null)
			return;
		
		if((perm instanceof BPMTypedPermission)) {
			
			BPMTypedHandler handler = getHandlers().get(((BPMTypedPermission)perm).getType());
			
			if(handler != null) {
				
				handler.handle(perm);
				
			} else {
				throw new AccessControlException("No handler resolved for the permission type="+((BPMTypedPermission)perm).getType());
			}
			
		} else 
			throw new IllegalArgumentException("Only permissions implementing cool interfaces are supported");
		
//		TODO: see permissionsfactory javadoc
		/*
		if((perm instanceof BPMTaskAccessPermission)) {
			//checkTaskAccessPermission(fctx, (BPMTaskAccessPermission)perm);
		} else if((perm instanceof BPMRightsMgmtPermission)) {
			//checkRightsMgmtPermission(fctx, (BPMRightsMgmtPermission)perm);
		} else if((perm instanceof BPMRoleAccessPermission)) {
			//checkRoleAccessPermission(fctx, (BPMRoleAccessPermission)perm);
		} else if((perm instanceof BPMGenericAccessPermission)) {
			//checkGenericAccessPermission(fctx, (BPMGenericAccessPermission)perm);
		} else 
			throw new IllegalArgumentException("Only permissions implementing cool interfaces are supported");
		 */
	}
	
	/*
	protected boolean fallsInGroups(int userId, List<NativeIdentityBind> nativeIdentities) {
	
		try {
			UserBusiness ub = getUserBusiness();
			@SuppressWarnings("unchecked")
			Collection<Group> userGroups = ub.getUserGroups(userId);
			
			if(userGroups != null) {
			
				for (Group group : userGroups) {
					
					String groupId = group.getPrimaryKey().toString();
					
					for (NativeIdentityBind nativeIdentity : nativeIdentities) {
						
						if(nativeIdentity.getIdentityType() == IdentityType.GROUP && nativeIdentity.getIdentityId().equals(groupId))
							return true;
					}
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
	*/
	
	
	/*
//	TODO: make this more effective, as it was changed in a rush
	protected void checkPermissionsForTaskInstance(int userId, TaskInstance taskInstance, Collection<Access> accesses, String variableIdentifier) throws AccessControlException {
		
		Long taskId = taskInstance.getTask().getId();
		Long taskInstanceId = taskInstance.getId();
		
		final List<ActorPermissions> allPerms = getBpmBindsDAO().getResultList(ActorPermissions.getSetByTaskIdOrTaskInstanceId, ActorPermissions.class,
				new Param(ActorPermissions.taskIdProperty, taskId),
				new Param(ActorPermissions.taskInstanceIdProperty, taskInstanceId)
		);
		
		if(allPerms != null) {
			
			boolean checkWriteAccess = accesses.contains(Access.write);
			boolean checkReadAccess = accesses.contains(Access.read);
			
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			
			AccessController ac = getAccessController();
			
			HashSet<ProcessRole> roles = new HashSet<ProcessRole>(allPerms.size()+10);
			
			for (ActorPermissions perm : allPerms) {
				
				if(perm.getProcessRoles() != null)
					roles.addAll(perm.getProcessRoles());
			}
			
			for (ProcessRole processRole : roles) {
				
				List<ActorPermissions> perms = processRole.getActorPermissions();
				
				if(perms != null) {
					
					Boolean hasTaskInstanceScopeAccess = null;
					Boolean hasTaskInstanceScopeANDVarAccess = null;
					Boolean hasTaskScopeAccess = null;
					Boolean hasTaskScopeANDVarAccess = null;
					
					for (ActorPermissions perm : perms) {
						
						if(taskInstanceId.equals(perm.getTaskInstanceId())) {

							if(variableIdentifier != null) {

								if(variableIdentifier.equals(perm.getVariableIdentifier())) {
									
									hasTaskInstanceScopeANDVarAccess = (!checkReadAccess || (perm.getReadPermission() != null && perm.getReadPermission()))
																	&& (!checkWriteAccess || (perm.getWritePermission() != null && perm.getWritePermission()));
									break;
								}
							}
							
							if(perm.getVariableIdentifier() == null) {
								
								hasTaskInstanceScopeAccess = (!checkReadAccess || (perm.getReadPermission() != null && perm.getReadPermission()))
															&& (!checkWriteAccess || (perm.getWritePermission() != null && perm.getWritePermission()));
								
								if(variableIdentifier == null)
									break;
							}
							
						} else if(taskId.equals(perm.getTaskId()) && perm.getTaskInstanceId() == null) {
							
							if(variableIdentifier != null) {

								if(variableIdentifier.equals(perm.getVariableIdentifier())) {
									
									hasTaskScopeANDVarAccess = (!checkReadAccess || (perm.getReadPermission() != null && perm.getReadPermission()))
																	&& (!checkWriteAccess || (perm.getWritePermission() != null && perm.getWritePermission()));
								}
							}
							
							if(perm.getVariableIdentifier() == null) {
								
								hasTaskScopeAccess = (!checkReadAccess || (perm.getReadPermission() != null && perm.getReadPermission()))
															&& (!checkWriteAccess || (perm.getWritePermission() != null && perm.getWritePermission()));
							}
						}
					}

					if(
							(variableIdentifier != null && (
										(hasTaskInstanceScopeANDVarAccess != null && hasTaskInstanceScopeANDVarAccess) ||
										(hasTaskInstanceScopeANDVarAccess == null && hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess) ||
										(hasTaskInstanceScopeANDVarAccess == null && hasTaskInstanceScopeAccess == null && hasTaskScopeANDVarAccess != null && hasTaskScopeANDVarAccess) ||
										(hasTaskInstanceScopeANDVarAccess == null && hasTaskInstanceScopeAccess == null && hasTaskScopeANDVarAccess == null && hasTaskScopeAccess != null && hasTaskScopeAccess)
									)
							) ||
							(variableIdentifier == null && (
									(hasTaskInstanceScopeAccess != null && hasTaskInstanceScopeAccess) ||
									(hasTaskInstanceScopeAccess == null && hasTaskScopeAccess != null && hasTaskScopeAccess)
								)
							)
					) {
						
						List<NativeIdentityBind> nativeIdentities = processRole.getNativeIdentities();
						
						if(checkFallsInRole(processRole.getProcessRoleName(), nativeIdentities, userId, ac, iwac))
							return;
					}
				}
			}
		}
		
		throw new AccessControlException("No permission for user="+userId+", for taskInstance="+taskInstance.getId()+", variableIdentifier="+variableIdentifier);
	}
	
	private boolean checkFallsInRole(String roleName, List<NativeIdentityBind> nativeIdentities, int userId, AccessController ac, IWApplicationContext iwac) {
		
		if(nativeIdentities != null && !nativeIdentities.isEmpty()) {
			if(fallsInUsers(userId, nativeIdentities) || fallsInGroups(userId, nativeIdentities))
				return true;
		} else {
			
			try {
				User usr = getUserBusiness().getUser(userId);
				@SuppressWarnings("unchecked")
				Set<String> roles = ac.getAllRolesForUser(usr);
				
				if(roles.contains(roleName))
					return true;
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
		}
		
		return false;
	}
	*/

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

	public void close() { }

	public Map<String, BPMTypedHandler> getHandlers() {
		return handlers;
	}
}