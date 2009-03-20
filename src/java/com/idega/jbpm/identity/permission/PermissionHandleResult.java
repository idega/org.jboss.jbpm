package com.idega.jbpm.identity.permission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/20 19:19:43 $ by $Author: civilis $
 */
public class PermissionHandleResult {
	
	private String message;
	private PermissionHandleResultStatus status;
	
	public enum PermissionHandleResultStatus {
		
		noAccess, hasAccess
	}
	
	public PermissionHandleResult() {
	}
	
	public PermissionHandleResult(PermissionHandleResultStatus status) {
		this.status = status;
	}
	
	public PermissionHandleResult(PermissionHandleResultStatus status,
	                              String message) {
		this.status = status;
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public PermissionHandleResultStatus getStatus() {
		return status;
	}
	
	public void setStatus(PermissionHandleResultStatus status) {
		this.status = status;
	}
}