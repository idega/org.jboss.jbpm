package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.List;

import com.idega.jbpm.identity.permission.Access;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2008/03/11 20:14:26 $ by $Author: civilis $
 */
public class Role {
	
	public static final String roleNameProperty = "roleName";
	private static final List<String> specificRoles = new ArrayList<String>(1);
	
	static {
		specificRoles.add("owner");
	}
	
	private String roleName;
	
	private List<Access> accesses;
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
	 * 
	 * @return does this role pertain only to general or process definition scope roles. 
	 * e.g. owner is not a general role, as the identity assignments make sense only for process instance scope (the owner user id)
	 */
	public boolean isGeneral() {
		return !specificRoles.contains(getRoleName());
	}
}