package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/12 10:58:30 $ by $Author: civilis $
 */
public interface BPMTypedPermission {

	public abstract String getType();
	
	public void setAttribute(String key, Object value);
	
	public <T>T getAttribute(String key);
	
	public boolean containsAttribute(String key);
}