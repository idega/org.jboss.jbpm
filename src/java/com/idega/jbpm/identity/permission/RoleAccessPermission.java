package com.idega.jbpm.identity.permission;

import java.security.Permission;

/**
 * Meant to check if current user belongs to process role
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/12 18:30:17 $ by $Author: civilis $
 */
public class RoleAccessPermission extends org.jbpm.security.permission.SubmitTaskParametersPermission implements BPMRoleAccessPermission {
	
	private static final long serialVersionUID = -6147266604847454693L;
	private Long processInstanceId;
	private String roleName;

	public RoleAccessPermission(String name, String actions) {
		super(name, actions);
	}
	
	@Override
	public boolean implies(Permission permission) {

		return false;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
}