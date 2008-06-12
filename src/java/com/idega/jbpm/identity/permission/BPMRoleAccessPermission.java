package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/12 18:30:18 $ by $Author: civilis $
 */
public interface BPMRoleAccessPermission {

	public abstract Long getProcessInstanceId();
	
	public abstract String getRoleName();
}