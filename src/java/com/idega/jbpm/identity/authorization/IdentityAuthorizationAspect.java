package com.idega.jbpm.identity.authorization;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
 */
@Aspect
public class IdentityAuthorizationAspect {
	
	@Before("com.idega.jbpm.business.BPMPointcuts.startProcessAtProcessManager() && args(processDefinitionId, ..)")
	public void checkPermissionToStartProcess(JoinPoint p, long processDefinitionId) {
		
		System.out.println("in aspect, advising for process def: "+processDefinitionId);
	}
}