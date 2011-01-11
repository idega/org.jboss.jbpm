package com.idega.jbpm.identity.authorization;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.idega.jbpm.identity.permission.PermissionHandleResult;
import com.idega.jbpm.identity.permission.PermissionHandleResult.PermissionHandleResultStatus;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.39 $ Last modified: $Date: 2009/03/20 19:19:08 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class IdentityAuthorizationService implements AuthorizationService {
	
	private static final long serialVersionUID = -7496842155073961922L;
	
	private static final Logger LOGGER = Logger.getLogger(IdentityAuthorizationService.class.getName());
	
	private AuthenticationService authenticationService;
	private BPMDAO bpmBindsDAO;
	private Map<String, BPMTypedHandler> handlers;
	
	@Autowired(required = false)
	public void setHandlers(List<BPMTypedHandler> handlers) {
		if (handlers == null) {
			LOGGER.warning("No handlers provided");
			return;
		}
			
		this.handlers = new HashMap<String, BPMTypedHandler>();
		for (BPMTypedHandler handler : handlers) {
			String[] handledTypes = handler.getHandledTypes();
			if (handledTypes != null) {
				for (String handledType : handledTypes) {
					if (!StringUtil.isEmpty(handledType)) {
						this.handlers.put(handledType, handler);
					}
				}
			} else
				LOGGER.warning("Typed permissions handler registered, but no supported permissions types returned. Handler class=" + handler.getClass().getName());
		}
	}
	
	@Transactional(readOnly = true, noRollbackFor = { AccessControlException.class })
	public void checkPermission(Permission perm) throws AccessControlException {
		if (!(perm instanceof BPMTypedPermission)) {
			throw new IllegalArgumentException("Expected permission type: " + BPMTypedPermission.class + ", received: " + perm);
		}
		
		BPMTypedPermission bpmPerm = (BPMTypedPermission) perm;
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		// faces context == null when permission is called by the system (i.e. not the result of user navigation)
		if (fctx == null){
			//if there is no httprequest and no credentials either in the permission object this must be the system doing stuff so allow it.
			//(eiki) I added this small change so not to bust up something that was working before.
			//But this should be taken under review. I didn't have time to test more with the fctx==null check totally removed but it seemed to work.
			//Vytautas suggested that async operations might fail if this wasn't here.
			 if (bpmPerm.getUserId() == null) {
				 return;
			 }
		}
		
		
		BPMTypedHandler handler = getHandlers().get(bpmPerm.getType());
		if (handler == null) {
			throw new AccessControlException("No handler resolved for the permission type=" + bpmPerm.getType());
		}
				
		PermissionHandleResult result = handler.handle(perm);
		if (result.getStatus() == PermissionHandleResultStatus.noAccess) {
			String message = getErrorMessage(bpmPerm, handler, result);
			throw new AccessControlException(message);
		}				
	}
	
	private String getErrorMessage(BPMTypedPermission bpmPerm, BPMTypedHandler handler, PermissionHandleResult result) {
		User user = null;
		Set<String> userRoles = null;
		Integer userId = bpmPerm.getUserId();
		if (userId != null) {
			try {
				user = getUserBusiness().getUser(userId);
				userRoles = getAccessController().getAllRolesForUser(user);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String message = "Access is denied! Permission for user: " + user + " (ID: " + userId + ", user roles: " + (userRoles == null ? "unknown" : userRoles) +
			"), BPM permission: " + bpmPerm + ". Handler: " + handler.getClass();
		message = message + ". " + (StringUtil.isEmpty(result.getMessage()) ? ("No access resolved by handler=" + handler) : result.getMessage());
		return message;
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
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected AccessController getAccessController() {
		return getIWMA().getAccessController();
	}
	
	protected IWMainApplication getIWMA() {
		FacesContext fctx = FacesContext.getCurrentInstance();
		return fctx == null ? IWMainApplication.getDefaultIWMainApplication() : IWMainApplication.getIWMainApplication(fctx);
	}
	
	public void close() {
	}
	
	public Map<String, BPMTypedHandler> getHandlers() {
		return handlers;
	}
}