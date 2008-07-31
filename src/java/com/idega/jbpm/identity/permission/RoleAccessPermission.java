package com.idega.jbpm.identity.permission;

import java.security.Permission;

/**
 * Meant to check if current user belongs to process role
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/07/31 10:56:29 $ by $Author: civilis $
 */
public class RoleAccessPermission extends org.jbpm.security.permission.SubmitTaskParametersPermission implements BPMRoleAccessPermission {
	
	private static final long serialVersionUID = -6147266604847454693L;
	private Long processInstanceId;
	private String roleName;
	private boolean checkContactsForRole = false;

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

	/**
	 * used to check, if the current role has permission to see contact of contactRolename
	 */
	public boolean isCheckContactsForRole() {
		return checkContactsForRole;
	}

	public void setCheckContactsForRole(boolean checkContactsForRole) {
		this.checkContactsForRole = checkContactsForRole;
	}
}