package com.idega.jbpm.identity.permission;

import java.security.BasicPermission;
import java.security.Permission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/05 07:23:09 $ by $Author: civilis $
 */
public class GenericAccessPermission extends BasicPermission implements BPMGenericAccessPermission {
	
	private static final long serialVersionUID = -7581102297895172710L;
	private Long processInstanceId;
	private Access access;

	public GenericAccessPermission(String name, String actions) {
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

	public Access getAccess() {
		return access;
	}

	public void setAccess(Access access) {
		this.access = access;
	}
}