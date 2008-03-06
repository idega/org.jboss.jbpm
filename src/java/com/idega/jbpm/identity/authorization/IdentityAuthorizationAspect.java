package com.idega.jbpm.identity.authorization;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import com.idega.jbpm.business.BPMPointcuts;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/06 11:55:03 $ by $Author: civilis $
 */
@Aspect
public class IdentityAuthorizationAspect {
	

	/*
	@Before(BPMPointcuts.startProcessAtProcessManager+" && args(processDefinitionId, ..)")
	public void checkPermissionToStartProcess(JoinPoint p, long processDefinitionId) {

//		TODO: permitting everyone to start process
	}
	*/
	
	@Before("("+BPMPointcuts.loadTaskInstanceViewAtViewManager+" || "+BPMPointcuts.submitProcessAtProcessManager+") && args(taskInstanceId, ..)")
	public void checkPermissionToTaskInstance(long taskInstanceId) {

		//System.out.println("checking if current user has access to task instance for: "+taskInstanceId);
		System.out.println("checking if current user has access to task instance for: "+taskInstanceId);
	}
}