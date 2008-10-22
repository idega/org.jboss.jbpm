package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/10/22 15:13:10 $ by $Author: civilis $
 */
public interface BPMTypedPermission {

	public abstract String getType();
	
	public void setAttribute(String key, Object value);
	
	public <T>T getAttribute(String key);
	
	public boolean containsAttribute(String key);
	
	/**
	 * user id to check against - if it's null, current logged in user is used
	 * @return
	 */
	public abstract Integer getUserId();
}