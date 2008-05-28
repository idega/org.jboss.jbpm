package com.idega.jbpm.identity.authorization;

import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.persistence.Param;
import com.idega.data.IDORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.BPMRightsMgmtPermission;
import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;
import com.idega.jbpm.identity.permission.BPMTaskVariableAccessPermission;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.24 $
 *
 * Last modified: $Date: 2008/05/28 08:02:47 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
@Transactional(readOnly=true)
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BPMDAO bpmBindsDAO;
	
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
	
	protected void checkRightsMgmtPermission(FacesContext fctx, BPMRightsMgmtPermission permission) throws AccessControlException {
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
//		super admin always gets an access
		if(!IWContext.getIWContext(fctx).isSuperAdmin()) {
		
			Long processInstanceId = permission.getProcessInstanceId();
			
			if(processInstanceId == null)
				throw new RuntimeException("No process instance id found in permission="+permission.getClass().getName());
			
			String roleName = "bpm_handler";
//			TODO: get roles that can edit permissions
			
			List<ProcessRole> proles = 
				getBpmBindsDAO().getResultList(ProcessRole.getSetByRoleNamesAndPIId, ProcessRole.class,
						new Param(ProcessRole.processInstanceIdProperty, processInstanceId),
						new Param(ProcessRole.processRoleNameProperty, Arrays.asList(new String[] {roleName}))
				);
			
			if(proles == null || proles.isEmpty())
				throw new AccessControlException("No process role found by role name="+roleName+" and process instance id = "+processInstanceId);
			
			ProcessRole prole = proles.iterator().next();
			
			try {
				
				int userId = new Integer(loggedInActorId);

				UserBusiness userBusiness = getUserBusiness();
				@SuppressWarnings("unchecked")
				Collection<Group> usrGrps = userBusiness.getUserGroups(userId);
				AccessController ac = getAccessController();
				IWApplicationContext iwac = getIWMA().getIWApplicationContext();
				
				List<NativeIdentityBind> nativeIdentities = prole.getNativeIdentities();
				
				if(checkFallsInRole(prole.getProcessRoleName(), nativeIdentities, usrGrps, userId, ac, iwac))
					return;
				
			} catch (RemoteException e) {
				throw new IBORuntimeException(e);
			}
			
			throw new AccessControlException("No rights management permission for user="+loggedInActorId+", for processInstanceId="+processInstanceId);
		}
	}

	public void checkPermission(Permission perm) throws AccessControlException {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		
//		faces context == null when permission is called by the system (i.e. not the result of user navigation)
		if(fctx == null)
			return;
		
		if((perm instanceof BPMTaskAccessPermission)) {
			checkTaskAccessPermission(fctx, (BPMTaskAccessPermission)perm);
		} else if((perm instanceof BPMRightsMgmtPermission)) {
			checkRightsMgmtPermission(fctx, (BPMRightsMgmtPermission)perm);
		} else 
			throw new IllegalArgumentException("Only permissions implementing "+BPMTaskAccessPermission.class.getName()+" or "+BPMRightsMgmtPermission.class.getName()+" supported");
	}
	
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
	
	protected void checkPermissionsForTaskInstance(int userId, TaskInstance taskInstance, Collection<Access> accesses, String variableIdentifier) throws AccessControlException {
		
		long processInstanceId = taskInstance.getProcessInstance().getId();
		
		List<ProcessRole> roles = 
			getBpmBindsDAO().getResultList(ProcessRole.getSetByPIId, ProcessRole.class,
					new Param(ProcessRole.processInstanceIdProperty, processInstanceId)
			);
		
		boolean checkWriteAccess = accesses.contains(Access.write);
		boolean checkReadAccess = accesses.contains(Access.read);
		
		try {
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			UserBusiness userBusiness = getUserBusiness();
			
			@SuppressWarnings("unchecked")
			Collection<Group> usrGrps = userBusiness.getUserGroups(userId);
			Long taskId = taskInstance.getTask().getId();
			Long taskInstanceId = taskInstance.getId();
			
			AccessController ac = getAccessController();
			
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
						
						if(checkFallsInRole(processRole.getProcessRoleName(), nativeIdentities, usrGrps, userId, ac, iwac))
							return;
						/*
						if(nativeIdentities != null && !nativeIdentities.isEmpty()) {
							if(fallsInUsers(userId, nativeIdentities) || fallsInGroups(userId, nativeIdentities))
								return;
						} else if(usrGrps != null) {
						
							String roleName = processRole.getProcessRoleName();
							
							@SuppressWarnings("unchecked")
							Collection<Group> grps = ac.getAllGroupsForRoleKey(roleName, iwac);
							
							for (Group roleGrp : grps) {
								
								if(usrGrps.contains(roleGrp)) {
									
//									falls in group satisfying permission
									return;
								}
							}
						}
						*/
					}
				}
			}
			
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
		
		throw new AccessControlException("No permission for user="+userId+", for taskInstance="+taskInstance.getId()+", variableIdentifier="+variableIdentifier);
	}
	
	private boolean checkFallsInRole(String roleName, List<NativeIdentityBind> nativeIdentities, Collection<Group> usrGrps, int userId, AccessController ac, IWApplicationContext iwac) {
		
		if(nativeIdentities != null && !nativeIdentities.isEmpty()) {
			if(fallsInUsers(userId, nativeIdentities) || fallsInGroups(userId, nativeIdentities))
				return true;
		} else if(usrGrps != null) {
		
			@SuppressWarnings("unchecked")
			Collection<Group> grps = ac.getAllGroupsForRoleKey(roleName, iwac);
			
			for (Group roleGrp : grps) {
				
				if(usrGrps.contains(roleGrp)) {
					
//					falls in group satisfying permission
					return true;
				}
			}
		}
		
		return false;
	}

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
}