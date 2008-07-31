package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/07/31 10:56:29 $ by $Author: civilis $
 */
public interface BPMRoleAccessPermission {

	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
	
	public abstract String getRoleName();
	
	public abstract void setRoleName(String roleName);
	
	public abstract boolean isCheckContactsForRole();

	public abstract void setCheckContactsForRole(boolean checkContactsForRole);
}