package com.idega.jbpm.identity;

import java.util.List;

import com.idega.jbpm.identity.RolesAssiger.Access;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
 */
public class Role {
	
	public static final String roleNameProperty = "roleName";
	
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
}