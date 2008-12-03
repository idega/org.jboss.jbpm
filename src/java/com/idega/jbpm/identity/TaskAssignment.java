package com.idega.jbpm.identity;

import java.util.List;

/**
 * reflects json task assignment expression
 * 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/12/03 12:04:17 $ by $Author: civilis $
 */
public class TaskAssignment {

	private Long rolesFromProcessInstanceId;
	private List<Role> roles;

	public List<Role> getRoles() {
		return roles;
	}

	/**
	 * optional
	 * 
	 * @return process instance id where roles are coming from. If set, this
	 *         process instance is used when resolving/creating actors for roles
	 */
	public Long getRolesFromProcessInstanceId() {
		return rolesFromProcessInstanceId;
	}
}