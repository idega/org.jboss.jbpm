package com.idega.jbpm.business;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/08/28 12:09:23 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
@Aspect
public class BPMPointcuts {

	public static final String startProcessAtProcessDefinitionW = "com.idega.jbpm.business.BPMPointcuts.startProcessAtProcessDefinitionW()";
	@Pointcut("execution(* com.idega.jbpm.exe.ProcessDefinitionW.startProcess(..))")
	public void startProcessAtProcessDefinitionW() { }
	
	public static final String submitAtTaskInstanceW = "com.idega.jbpm.business.BPMPointcuts.submitAtTaskInstanceW()";
	@Pointcut("execution(* com.idega.jbpm.exe.TaskInstanceW.submit(..))")
	public void submitAtTaskInstanceW() { }
	
	public static final String loadInitViewAtProcessDefinitionW = "com.idega.jbpm.business.BPMPointcuts.loadInitViewAtProcessDefinitionW()";
	@Pointcut("execution(* com.idega.jbpm.exe.ProcessDefinitionW.loadInitView(..))")
	public void loadInitViewAtProcessDefinitionW() { }
	
	public static final String loadViewAtTaskInstanceW = "com.idega.jbpm.business.BPMPointcuts.loadViewAtTaskInstanceW()";
	@Pointcut("execution(* com.idega.jbpm.exe.TaskInstanceW.loadView(..))")
	public void loadViewAtTaskInstanceW() { }
	
	public static final String setContactsPermissionAtProcessInstanceW = "com.idega.jbpm.business.BPMPointcuts.setContactsPermissionAtProcessInstanceW()";
	@Pointcut("execution(* com.idega.jbpm.exe.ProcessInstanceW.setContactsPermission(..))")
	public void setContactsPermissionAtProcessInstanceW() { }
	
	public static final String setTaskRolePermissionsAtTaskInstanceW = "com.idega.jbpm.business.BPMPointcuts.setTaskRolePermissionsAtTaskInstanceW()";
	@Pointcut("execution(* com.idega.jbpm.exe.TaskInstanceW.setTaskRolePermissions(..))")
	public void setTaskRolePermissionsAtTaskInstanceW() { }
}