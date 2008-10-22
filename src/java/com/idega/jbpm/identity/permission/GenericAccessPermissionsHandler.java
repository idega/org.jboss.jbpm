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
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.RolesManager;
import com.idega.presentation.IWContext;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/10/22 15:13:46 $ by $Author: civilis $
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
	
	public static final String processInstanceIdAtt = 				"processInstanceId";
	public static final String userAtt = 							"user";

	public String[] getHandledTypes() {
		return new String[] {PermissionsFactoryImpl.genericAccessPermType};
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
		
		if(IWContext.getCurrentInstance().isSuperAdmin())
			return;
		
		Long processInstanceId = permission.getAttribute(processInstanceIdAtt);
		
		List<Actor> proles = 
			getBpmBindsDAO().getResultList(Actor.getRolesHavingCaseHandlerRights, Actor.class,
					new Param(Actor.processInstanceIdProperty, processInstanceId)
			);
		
		if(proles != null) {
			
			AccessController ac = getAccessController();
			IWApplicationContext iwac = getIWMA().getIWApplicationContext();
			
			for (Actor processRole : proles) {
				
				if(getRolesManager().checkFallsInRole(processRole.getProcessRoleName(), processRole.getNativeIdentities(), userId, ac, iwac))
					return;
			}
		}
		
		throw new AccessControlException("No generic access for user="+userId);
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