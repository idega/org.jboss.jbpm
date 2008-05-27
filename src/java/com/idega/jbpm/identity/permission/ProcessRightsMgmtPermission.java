package com.idega.jbpm.identity.permission;

import java.security.Permission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/27 18:04:46 $ by $Author: civilis $
 */
public class ProcessRightsMgmtPermission extends org.jbpm.security.permission.SubmitTaskParametersPermission implements BPMRightsMgmtPermission {
	
	private static final long serialVersionUID = -1664594715704314668L;
	private boolean changeTaskRights = false;
	private Long processInstanceId;

	public ProcessRightsMgmtPermission(String name, String actions) {
		super(name, actions);
	}
	
	@Override
	public boolean implies(Permission permission) {

		return false;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public boolean isChangeTaskRights() {
		return changeTaskRights;
	}

	public void setChangeTaskRights(boolean changeTaskRights) {
		this.changeTaskRights = changeTaskRights;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
}