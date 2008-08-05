package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/05 07:23:09 $ by $Author: civilis $
 */
public interface BPMGenericAccessPermission {

	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
	
	public abstract Access getAccess();
	
	public abstract void setAccess(Access access);
}