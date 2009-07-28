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
import com.idega.jbpm.identity.permission.PermissionHandleResult;
import com.idega.jbpm.identity.permission.PermissionHandleResult.PermissionHandleResultStatus;
import com.idega.user.business.UserBusiness;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.39 $ Last modified: $Date: 2009/03/20 19:19:08 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class IdentityAuthorizationService implements AuthorizationService {
	
	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;
	private BPMDAO bpmBindsDAO;
	private Map<String, BPMTypedHandler> handlers;
	
	@Autowired(required = false)
	public void setHandlers(List<BPMTypedHandler> handlers) {
		
		if (handlers != null) {
			
			// double size here, as some handlers might support more than one permission type
			this.handlers = new HashMap<String, BPMTypedHandler>(handlers
			        .size() * 2);
			
			for (BPMTypedHandler handler : handlers) {
				
				String[] handledTypes = handler.getHandledTypes();
				
				if (handledTypes != null) {
					
					for (String handledType : handledTypes) {
						
						if (handledType != null
						        && !CoreConstants.EMPTY.equals(handledType))
							this.handlers.put(handledType, handler);
					}
					
				} else
					Logger
					        .getLogger(getClass().getName())
					        .log(
					            Level.WARNING,
					            "Typed permissions handler registered, but no supported permissions types returned. Handler class="
					                    + handler.getClass().getName());
			}
		}
	}
	
	@Transactional(readOnly = true, noRollbackFor = { AccessControlException.class })
	public void checkPermission(Permission perm) throws AccessControlException {
		
		if ((perm instanceof BPMTypedPermission)) {

			FacesContext fctx = FacesContext.getCurrentInstance();
			// faces context == null when permission is called by the system (i.e. not the result of user navigation)
			if (fctx == null){
				//if there is no httprequest and no credentials either in the permission object this must be the system doing stuff so allow it.
				//(eiki) I added this small change so not to bust up something that was working before.
				//But this should be taken under review. I didn't have time to test more with the fctx==null check totally removed but it seemed to work.
				//Vytautas suggested that async operations might fail if this wasn't here.
				 if(((BPMTypedPermission) perm).getUserId()==null){
					 return;
				 }
			}
			
			BPMTypedHandler handler = getHandlers().get(
			    ((BPMTypedPermission) perm).getType());
			
			if (handler != null) {
				
				PermissionHandleResult result = handler.handle(perm);
				
				if (result.getStatus() == PermissionHandleResultStatus.noAccess) {
					
					throw new AccessControlException(!StringUtil.isEmpty(result
					        .getMessage()) ? result.getMessage()
					        : "No access resolved by handler=" + handler);
				}
				
			} else {
				throw new AccessControlException(
				        "No handler resolved for the permission type="
				                + ((BPMTypedPermission) perm).getType());
			}
			
		} else
			throw new IllegalArgumentException(
			        "Only permissions implementing cool interfaces are supported");
	}
	
	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}
	
	@Autowired
	public void setAuthenticationService(
	        AuthenticationService authenticationService) {
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
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil
			        .getIWContext(), UserBusiness.class);
		} catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected AccessController getAccessController() {
		
		return getIWMA().getAccessController();
	}
	
	protected IWMainApplication getIWMA() {
		
		IWMainApplication iwma;
		FacesContext fctx = FacesContext.getCurrentInstance();
		
		if (fctx == null)
			iwma = IWMainApplication.getDefaultIWMainApplication();
		else
			iwma = IWMainApplication.getIWMainApplication(fctx);
		
		return iwma;
	}
	
	public void close() {
	}
	
	public Map<String, BPMTypedHandler> getHandlers() {
		return handlers;
	}
}