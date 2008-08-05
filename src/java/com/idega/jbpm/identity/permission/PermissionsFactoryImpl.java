package com.idega.jbpm.identity.permission;

import java.security.Permission;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/05 07:23:09 $ by $Author: civilis $
 */
@Service
@Scope("singleton")
public class PermissionsFactoryImpl implements PermissionsFactory {

	public Permission getTaskSubmitPermission(boolean authPooledActorsOnly, TaskInstance taskInstance) {
		
		SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		
		return permission;
	}
	
	public Permission getRightsMgmtPermission(Long processInstanceId) {
		
		ProcessRightsMgmtPermission permission = new ProcessRightsMgmtPermission("procRights", null);
		permission.setChangeTaskRights(true);
		permission.setProcessInstanceId(processInstanceId);
		
		return permission;
	}
	
	public Permission getAccessPermission(long processInstanceId, Access access) {
		
		GenericAccessPermission permission = new GenericAccessPermission("bpmGenericAccess", null);
		permission.setAccess(access);
		permission.setProcessInstanceId(processInstanceId);
		
		return permission;
	}
	
	public Permission getTaskViewPermission(boolean authPooledActorsOnly, TaskInstance taskInstance) {
		
		ViewTaskParametersPermission permission = new ViewTaskParametersPermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		
		return permission;
	}
	
	public Permission getTaskVariableViewPermission(boolean authPooledActorsOnly, TaskInstance taskInstance, String variableIdentifier) {
		
		ViewTaskVariablePermission permission = new ViewTaskVariablePermission("taskInstance", null, taskInstance);
		permission.setCheckOnlyInActorsPool(authPooledActorsOnly);
		permission.setVariableIndentifier(variableIdentifier);
		
		return permission;
	}
	
	
}