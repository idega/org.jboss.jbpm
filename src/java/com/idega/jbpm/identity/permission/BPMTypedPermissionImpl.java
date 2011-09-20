package com.idega.jbpm.identity.permission;

import java.security.BasicPermission;
import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/10/22 15:13:10 $ by $Author: civilis $
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

	public Integer getUserId() {
		Object object = getAttribute(GenericAccessPermissionsHandler.userAtt);
		if (object instanceof Integer)
			return (Integer) object;
		if (object instanceof User)
			return Integer.valueOf(((User) object).getId());
		
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": type: " + getType() + ", attributes: " + attributes;
	}
}