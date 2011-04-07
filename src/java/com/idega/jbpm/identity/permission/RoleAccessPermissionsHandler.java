package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.persistence.Param;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.UnavailableIWContext;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionHandleResult.PermissionHandleResultStatus;
import com.idega.presentation.IWContext;
import com.idega.util.ListUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $ Last modified: $Date: 2009/03/20 19:19:43 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class RoleAccessPermissionsHandler implements BPMTypedHandler {
	
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private RolesManager rolesManager;
	
	public static final String processInstanceIdAtt = "processInstanceId";
	public static final String roleNameAtt = "roleName";
	public static final String checkContactsForRoleAtt = "checkContactsForRole";
	
	public String[] getHandledTypes() {
		return new String[] { PermissionsFactoryImpl.roleAccessPermType };
	}
	
	// TODO: refactor this monster method
	@SuppressWarnings("unused")
	public PermissionHandleResult handle(Permission perm) {
		
		if (!(perm instanceof BPMTypedPermission)) {
			
			throw new IllegalArgumentException("Unsupported permission type="
			        + perm.getClass().getName());
		}
		
		BPMTypedPermission permission = (BPMTypedPermission) perm;
		PermissionHandleResult result = null;
		
		Integer userId = permission.getUserId();
		
		if (userId == null) {
			
			String loggedInActorId = getAuthenticationService().getActorId();
			
			if (loggedInActorId == null) {
				result = new PermissionHandleResult(
				        PermissionHandleResultStatus.noAccess, "Not logged in");
			} else {
				
				userId = new Integer(loggedInActorId);
			}
		}
		
		if (result == null) {
			
			AccessController ac = getAccessController();
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			
			Long processInstanceId = permission
			        .getAttribute(processInstanceIdAtt);
			String roleName = permission.getAttribute(roleNameAtt);
			Boolean checkContactsForRole = permission
			        .getAttribute(checkContactsForRoleAtt);
			
			if (checkContactsForRole == null)
				checkContactsForRole = false;
			
			if (checkContactsForRole) {
				
				// super admin always gets an access
				if (isCurrentUserSuperAdmin()) {
					
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.hasAccess);
					
				} else {
					
					// get all roles, the user can see/not see
					Collection<Role> rolesUserCanSee = getRolesManager()
					        .getUserPermissionsForRolesContacts(
					            processInstanceId, userId);
					
					if (rolesUserCanSee != null) {
						
						Role roleToCheckFor = new Role(roleName);
						
						if (rolesUserCanSee.contains(roleToCheckFor)) {
							
							for (Role role : rolesUserCanSee) {
								
								if (roleToCheckFor.equals(role)) {
									
									if (role.getAccesses() != null
									        && role.getAccesses().contains(
									            Access.contactsCanBeSeen)) {
										
										result = new PermissionHandleResult(
										        PermissionHandleResultStatus.hasAccess);
										
										// TODO: get rid of those breaks
										break;
									}
									
									break;
								}
							}
							
						}
					}
					
					if (result == null) {
						result = new PermissionHandleResult(
						        PermissionHandleResultStatus.noAccess,
						        "No contacts for role access for user="
						                + userId + ", for processInstanceId="
						                + processInstanceId + ", for role="
						                + roleName);
					}
				}
				
			} else {
				
				// checking basic falling in role
				
				List<Actor> actors = getBpmBindsDAO().getResultList(
				    Actor.getSetByRoleNamesAndPIId,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty,
				            processInstanceId),
				    new Param(Actor.processRoleNameProperty, Arrays
				            .asList(new String[] { roleName })));
				
				if (ListUtil.isEmpty(actors)) {
					
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.noAccess,
					        "No actors found by role name=" + roleName
					                + " and process instance id = "
					                + processInstanceId);
				} else {
					
					Actor actor = actors.iterator().next();
					
					List<NativeIdentityBind> nativeIdentities = actor
					        .getNativeIdentities();
					
					if (getRolesManager().checkFallsInRole(
					    actor.getProcessRoleName(), nativeIdentities, userId,
					    ac, iwac)) {
						
						result = new PermissionHandleResult(
						        PermissionHandleResultStatus.hasAccess);
						
					} else {
						result = new PermissionHandleResult(
						        PermissionHandleResultStatus.noAccess,
						        "No role access for user=" + userId
						                + ", for processInstanceId="
						                + processInstanceId + ", for role="
						                + roleName);
					}
				}
			}
		}
		
		if (result == null) {
			
			result = new PermissionHandleResult(
			        PermissionHandleResultStatus.noAccess);
			Logger
			        .getLogger(getClass().getName())
			        .log(
			            Level.WARNING,
			            "In handling permission ("
			                    + perm
			                    + "), no result at the end retrieved. Returning no access");
		}
		
		return result;
	}
	
	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}
	
	public void setAuthenticationService(
	        AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}
	
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	private IWMainApplication getIWMA() {
		
		IWMainApplication iwma;
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		if (fctx == null)
			iwma = IWMainApplication.getDefaultIWMainApplication();
		else
			iwma = IWMainApplication.getIWMainApplication(fctx);
		
		return iwma;
	}
	
	private AccessController getAccessController() {
		
		return getIWMA().getAccessController();
	}
	
	public RolesManager getRolesManager() {
		return rolesManager;
	}
	
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
	
	protected boolean isCurrentUserSuperAdmin() {
		try {
			IWContext iwc = IWContext.getCurrentInstance();
			if(iwc!=null){
				return iwc.isSuperAdmin();
			}
		}
		catch (UnavailableIWContext e) {
			//we are not in a request
		}
		
		return false;
		
	}
}