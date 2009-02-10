package com.idega.jbpm.identity;

import java.util.Arrays;
import java.util.List;

import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.RoleScope;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 * 
 * Last modified: $Date: 2009/02/10 12:24:44 $ by $Author: civilis $
 */
public class Role {

	/**
	 * represents processInstanceId this role is for. optional. Should be used in the subprocesses everywhere.
	 */
	private Long processInstanceId;
	private String roleName;
	private List<String> assignIdentities;
	private RoleScope scope;
	private List<Access> accesses;
	private List<String> rolesContacts;
	
	public List<String> getRolesContacts() {
		return rolesContacts;
	}

	public void setRolesContacts(List<String> rolesContacts) {
		this.rolesContacts = rolesContacts;
	}

	public Role() {	}
	
	public Role(String roleName, Access... accesses) {
		
		this.roleName = roleName;
		setAccesses(accesses == null ? null : Arrays.asList(accesses));
	}
	
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	public List<Access> getAccesses() {
		
		return accesses;
	}
	public void setAccesses(List<Access> accesses) {
		this.accesses = accesses;
	}
	public List<String> getAssignIdentities() {
		return assignIdentities;
	}
	public void setAssignIdentities(List<String> assignIdentities) {
		this.assignIdentities = assignIdentities;
	}
	public RoleScope getScope() {
		return scope == null ? RoleScope.PD : scope;
	}
	public void setScope(RoleScope scope) {
		this.scope = scope;
	}
	
	@Override
	public int hashCode() {
		return getRoleName().hashCode();
	}
	
	@Override
	public boolean equals(Object arg0) {
		
		if(super.equals(arg0))
			return true;
		
		String roleName = arg0 instanceof String ? (String)arg0 : arg0 instanceof Role ? ((Role)arg0).getRoleName() : null;
		
		return roleName != null && roleName.equals(getRoleName());
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
}