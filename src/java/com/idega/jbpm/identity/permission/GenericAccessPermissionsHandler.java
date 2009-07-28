package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.List;

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
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionHandleResult.PermissionHandleResultStatus;
import com.idega.presentation.IWContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $ Last modified: $Date: 2009/03/20 19:19:43 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class GenericAccessPermissionsHandler implements BPMTypedHandler {
	
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private RolesManager rolesManager;
	
	public static final String processInstanceIdAtt = "processInstanceId";
	public static final String userAtt = "user";
	
	public String[] getHandledTypes() {
		return new String[] { PermissionsFactoryImpl.genericAccessPermType };
	}
	
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
			
			if (isCurrentUserSuperAdmin()) {
				
				result = new PermissionHandleResult(
				        PermissionHandleResultStatus.hasAccess);
				
			} else {
				
				Long processInstanceId = permission
				        .getAttribute(processInstanceIdAtt);
				
				List<Actor> actors = getBpmBindsDAO().getResultList(
				    Actor.getRolesHavingCaseHandlerRights,
				    Actor.class,
				    new Param(Actor.processInstanceIdProperty,
				            processInstanceId));
				
				if (actors != null) {
					
					AccessController ac = getAccessController();
					IWApplicationContext iwac = getIWMA()
					        .getIWApplicationContext();
					
					for (Actor actor : actors) {
						
						if (getRolesManager().checkFallsInRole(
						    actor.getProcessRoleName(),
						    actor.getNativeIdentities(), userId, ac, iwac)) {
							
							result = new PermissionHandleResult(
							        PermissionHandleResultStatus.hasAccess);
							break;
						}
					}
				}
				
				if (result == null)
					result = new PermissionHandleResult(
					        PermissionHandleResultStatus.noAccess,
					        "No generic access for user=" + userId);
			}
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