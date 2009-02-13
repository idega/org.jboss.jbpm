package com.idega.jbpm.identity;

import java.util.List;

/**
 * 
 * TODO: rename to RolesAssignment
 * reflects json task assignment expression
 * 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2009/02/13 17:27:48 $ by $Author: civilis $
 */
public class TaskAssignment {

	/**
	 * @deprecated getMainProcessInstanceId from bpmFactory should be used
	 */
	@Deprecated
	private Long rolesFromProcessInstanceId;
	private List<Role> roles;

	public List<Role> getRoles() {
		return roles;
	}

	/**
	 * @deprecated getMainProcessInstanceId from bpmFactory should be used
	 * optional
	 * 
	 * @return process instance id where roles are coming from. If set, this
	 *         process instance is used when resolving/creating actors for roles
	 */
	@Deprecated
	public Long getRolesFromProcessInstanceId() {
		return rolesFromProcessInstanceId;
	}
}