package com.idega.jbpm.identity.permission;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
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
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/12 10:58:30 $ by $Author: civilis $
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
	
	public static final String processInstanceIdAtt = 				"processInstanceId";
	public static final String roleNameAtt = 						"roleName";
	public static final String checkContactsForRoleAtt = 			"checkContactsForRole";

	public String[] getHandledTypes() {
		return new String[] {PermissionsFactoryImpl.roleAccessPermType};
	}

	public void handle(Permission perm) throws AccessControlException {
		
		if(!(perm instanceof BPMTypedPermission)) {
			
			throw new IllegalArgumentException("Unsupported permission type="+perm.getClass().getName());
		}
		
		BPMTypedPermission permission = (BPMTypedPermission)perm;
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
		AccessController ac = getAccessController();
		IWApplicationContext iwac = getIWMA().getIWApplicationContext();
		
		Long processInstanceId = permission.getAttribute(processInstanceIdAtt);
		String roleName = permission.getAttribute(roleNameAtt);
		Boolean checkContactsForRole = permission.getAttribute(checkContactsForRoleAtt);
		
		if(checkContactsForRole == null)
			checkContactsForRole = false;
		
		if(checkContactsForRole) {
			
//			super admin always gets an access
			if(!IWContext.getCurrentInstance().isSuperAdmin()) {
				
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
							if(getRolesManager().checkFallsInRole(prole.getProcessRoleName(), prole.getNativeIdentities(), userId, ac, iwac))
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
			
			if(getRolesManager().checkFallsInRole(prole.getProcessRoleName(), nativeIdentities, userId, ac, iwac))
				return;

			throw new AccessControlException("No role access for user="+loggedInActorId+", for processInstanceId="+processInstanceId+", for role="+roleName);
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