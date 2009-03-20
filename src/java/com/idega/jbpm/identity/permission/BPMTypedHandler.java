package com.idega.jbpm.identity.permission;

import java.security.Permission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/03/20 19:19:43 $ by $Author: civilis $
 */
public interface BPMTypedHandler {
	
	public abstract String[] getHandledTypes();
	
	public abstract PermissionHandleResult handle(Permission perm);
}