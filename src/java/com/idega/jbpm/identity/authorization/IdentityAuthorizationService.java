package com.idega.jbpm.identity.authorization;

import java.security.AccessControlException;
import java.security.Permission;

import org.jbpm.security.AuthenticationService;
import org.jbpm.security.AuthorizationService;

import com.idega.jbpm.identity.permission.BPMTaskAccessPermission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/07 13:26:41 $ by $Author: civilis $
 */
public class IdentityAuthorizationService implements AuthorizationService {

	private static final long serialVersionUID = -7496842155073961922L;
	
	private AuthenticationService authenticationService;

	public void checkPermission(Permission perm) throws AccessControlException {

		if(!(perm instanceof BPMTaskAccessPermission))
			throw new IllegalArgumentException("Only permissions implementing "+BPMTaskAccessPermission.class.getName()+" supported");
		
		BPMTaskAccessPermission permission = (BPMTaskAccessPermission)perm;
		
		String loggedInActorId = getAuthenticationService().getActorId();
		
		System.out.println("checking permission ....... for: "+loggedInActorId);
		System.out.println("permission: "+permission.getAccesses());
	}

	public void close() { }

	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
}