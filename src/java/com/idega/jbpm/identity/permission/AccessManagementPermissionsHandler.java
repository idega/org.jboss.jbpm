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
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/12 10:58:29 $ by $Author: civilis $
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
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		if(loggedInActorId == null)
			throw new AccessControlException("Not logged in");
		
//		super admin always gets an access
		if(!IWContext.getCurrentInstance().isSuperAdmin()) {
		
			Long processInstanceId = permission.getAttribute(processInstanceIdAtt);
			
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
				
				if(getRolesManager().checkFallsInRole(prole.getProcessRoleName(), nativeIdentities, userId, ac, iwac))
					return;
			}
			
			throw new AccessControlException("No access management permission for user="+loggedInActorId+", for processInstanceId="+processInstanceId);
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