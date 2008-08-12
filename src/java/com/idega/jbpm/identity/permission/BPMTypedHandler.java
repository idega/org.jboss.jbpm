package com.idega.jbpm.identity.permission;

import java.security.AccessControlException;
import java.security.Permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/12 10:58:30 $ by $Author: civilis $
 */
public interface BPMTypedHandler {

	public abstract String[] getHandledTypes();
	
	public abstract void handle(Permission perm) throws AccessControlException;
}