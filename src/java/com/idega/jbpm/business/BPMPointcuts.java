package com.idega.jbpm.business;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
 */
@Aspect
public class BPMPointcuts {

	@Pointcut("execution(* com.idega.jbpm.exe.ProcessManager.startProcess(..))")
	public void startProcessAtProcessManager() {
	}
}