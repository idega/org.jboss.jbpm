package com.idega.jbpm.identity;

import java.util.Arrays;
import java.util.List;

import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.RoleScope;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.14 $
 * 
 *          Last modified: $Date: 2009/02/26 08:53:13 $ by $Author: civilis $
 */
public class Role {

	private String roleName;
	/**
	 * @deprecated use identities property
	 */
	@Deprecated
	private List<String> assignIdentities;
	private List<Identity> identities;
	/**
	 * @deprecated not used anymore
	 * 
	 */
	@Deprecated
	private RoleScope scope;
	private List<Access> accesses;
	private List<String> rolesContacts;

	public List<String> getRolesContacts() {
		return rolesContacts;
	}

	public void setRolesContacts(List<String> rolesContacts) {
		this.rolesContacts = rolesContacts;
	}

	public Role() {
	}

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

	/**
	 * @deprecated use identities property
	 */
	@Deprecated
	public List<String> getAssignIdentities() {
		return assignIdentities;
	}

	public void setAssignIdentities(List<String> assignIdentities) {
		this.assignIdentities = assignIdentities;
	}

	@Deprecated
	public RoleScope getScope() {
		return scope == null ? RoleScope.PD : scope;
	}

	@Deprecated
	public void setScope(RoleScope scope) {
		this.scope = scope;
	}

	@Override
	public int hashCode() {
		return getRoleName().hashCode();
	}

	@Override
	public boolean equals(Object arg0) {

		if (super.equals(arg0))
			return true;

		String roleName = arg0 instanceof String ? (String) arg0
				: arg0 instanceof Role ? ((Role) arg0).getRoleName() : null;

		return roleName != null && roleName.equals(getRoleName());
	}

	public List<Identity> getIdentities() {
		return identities;
	}

	public void setIdentities(List<Identity> identities) {
		this.identities = identities;
	}
}