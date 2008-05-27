package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/27 18:04:46 $ by $Author: civilis $
 */
public interface BPMRightsMgmtPermission {

	public abstract boolean isChangeTaskRights();
	
	public abstract Long getProcessInstanceId();
}