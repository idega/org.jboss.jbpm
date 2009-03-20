package com.idega.jbpm.identity.permission;

import java.security.Permission;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionHandleResult.PermissionHandleResultStatus;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;

/**
 * checks, if the user provided falls in any of idega native roles provided
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/03/20 19:19:43 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class NativeRolesPermissionsHandler implements BPMTypedHandler {
	
	public static final String handlerType = "NativeRoles";
	public static final String rolesAtt = "nativeRoles";
	public static final String userAtt = "user";
	
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private RolesManager rolesManager;
	
	public String[] getHandledTypes() {
		return new String[] { handlerType };
	}
	
	public PermissionHandleResult handle(Permission perm) {
		
		if (!(perm instanceof BPMTypedPermission)) {
			
			throw new IllegalArgumentException("Unsupported permission type="
			        + perm.getClass().getName());
		}
		
		BPMTypedPermission permission = (BPMTypedPermission) perm;
		
		List<String> roles = permission.getAttribute(rolesAtt);
		User usr = (User) permission.getAttribute(userAtt);
		
		if (usr == null) {
			
			IWContext iwc = IWContext.getCurrentInstance();
			
			if (iwc != null)
				usr = iwc.getCurrentUser();
		}
		
		PermissionHandleResult result = null;
		
		if (usr != null && roles != null) {
			
			AccessController ac = getAccessController();
			
			Set<String> userRoles = ac.getAllRolesForUser(usr);
			
			if (userRoles != null) {
				
				for (String role : roles) {
					
					if (userRoles.contains(role)) {
						
						result = new PermissionHandleResult(
						        PermissionHandleResultStatus.hasAccess);
						break;
					}
				}
			}
			
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
			    "Something is missing. User=" + usr + ", roles=" + roles);
		}
		
		if (result == null)
			result = new PermissionHandleResult(
			        PermissionHandleResultStatus.noAccess, "User " + usr
			                + " doesn't fall in " + roles);
		
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
}