package com.idega.jbpm.identity.permission;

import java.security.AccessControlException;
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
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;
import com.idega.util.ListUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2009/01/23 11:44:38 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class AccessManagementPermissionsHandler implements BPMTypedHandler {
	
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private RolesManager rolesManager;
	
	public static final String processInstanceIdAtt = 				"processInstanceId";

	public String[] getHandledTypes() {
		return new String[] {PermissionsFactoryImpl.accessManagementPermType};
	}

	public void handle(Permission perm) throws AccessControlException {
		
		if(!(perm instanceof BPMTypedPermission)) {
			
			throw new IllegalArgumentException("Unsupported permission type="+perm.getClass().getName());
		}
		
		BPMTypedPermission permission = (BPMTypedPermission)perm;
		
		Integer userId = permission.getUserId();
		
		if(userId == null) {
		
			String loggedInActorId = getAuthenticationService().getActorId();
			
			if(loggedInActorId == null)
				throw new AccessControlException("Not logged in");
			
			userId = new Integer(loggedInActorId);
		}
		
//		super admin always gets an access
		if(!IWContext.getCurrentInstance().isSuperAdmin()) {
		
			Long processInstanceId = permission.getAttribute(processInstanceIdAtt);
			
//			TODO: use BPMDAOIMPL.getPermissionsForUser
			
			if(processInstanceId == null)
				throw new RuntimeException("No process instance id found in permission="+permission.getClass().getName());
			
			List<String> roleNames = getBpmBindsDAO().getResultList(Actor.getRoleNameHavingRightsModifyPermissionByPIId, String.class,
					new Param(Actor.processInstanceIdProperty, processInstanceId));
			
			if(roleNames != null && !roleNames.isEmpty()) {
			
				List<Actor> actorsForRoles = 
					getBpmBindsDAO().getResultList(Actor.getSetByRoleNamesAndPIId, Actor.class,
							new Param(Actor.processInstanceIdProperty, processInstanceId),
							new Param(Actor.processRoleNameProperty, roleNames)
					);
				
				if(!ListUtil.isEmpty(actorsForRoles)) {

					for (Actor actor : actorsForRoles) {
					
						AccessController ac = getAccessController();
						IWApplicationContext iwac = getIWMA().getIWApplicationContext();
						
						List<NativeIdentityBind> nativeIdentities = actor.getNativeIdentities();
						
						if(getRolesManager().checkFallsInRole(actor.getProcessRoleName(), nativeIdentities, userId, ac, iwac))
							return;
					}
				}
			}
			
			throw new AccessControlException("No access management permission for user="+userId+", for processInstanceId="+processInstanceId);
		}
	}
	
	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
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
		
		if(fctx == null)
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
}