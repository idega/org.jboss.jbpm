package com.idega.jbpm.identity;

import java.util.Arrays;
import java.util.List;

import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.identity.permission.RoleScope;

/**
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.16 $
 *
 * Last modified: $Date: 2009/05/25 13:44:27 $ by $Author: valdas $
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
	private List<String> rolesComments;
	private String userId;
	private boolean forTaskInstance = false;
	private Long processInstanceId;


	public List<String> getRolesContacts() {
		return rolesContacts;
	}

	public void setRolesContacts(List<String> rolesContacts) {
		this.rolesContacts = rolesContacts;
	}

	public Role() {
		super();
	}

	public Role(String roleName, Access... accesses) {
		this();

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

	public String getUserId() {
    	return userId;
    }

	public void setUserId(String userId) {
    	this.userId = userId;
    }

	public boolean getForTaskInstance() {
    	return forTaskInstance;
    }

	public void setForTaskInstance(boolean forTaskInstance) {
    	this.forTaskInstance = forTaskInstance;
    }

	public List<Identity> getIdentities() {
		return identities;
	}

	public void setIdentities(List<Identity> identities) {
		this.identities = identities;
	}

	public List<String> getRolesComments() {
		return rolesComments;
	}

	public void setRolesComments(List<String> rolesComments) {
		this.rolesComments = rolesComments;
	}

}