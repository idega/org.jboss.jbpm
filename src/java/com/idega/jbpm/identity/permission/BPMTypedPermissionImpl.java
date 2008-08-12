package com.idega.jbpm.identity.permission;

import java.security.BasicPermission;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/12 10:58:30 $ by $Author: civilis $
 */
public class BPMTypedPermissionImpl extends BasicPermission implements BPMTypedPermission {
	
	private static final long serialVersionUID = -6444311168247316542L;
	private Map<String, Object> attributes;
	private String type;

	public BPMTypedPermissionImpl(String name, String actions) {
		super(name, actions);
		attributes = new HashMap<String, Object>();
		type = name;
	}
	
	@Override
	public boolean implies(Permission permission) {

		return false;
	}

	public boolean containsAttribute(String key) {
		return attributes.containsKey(key);
	}

	public <T> T getAttribute(String key) {
		
		@SuppressWarnings("unchecked")
		T val = (T)attributes.get(key);
		
		return val;
	}

	public String getType() {
		return type;
	}

	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}
}