package com.idega.jbpm.business;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/06 11:55:03 $ by $Author: civilis $
 */
@Aspect
public class BPMPointcuts {

	public static final String startProcessAtProcessManager = "com.idega.jbpm.business.BPMPointcuts.startProcessAtProcessManager()";
	@Pointcut("execution(* com.idega.jbpm.exe.ProcessManager.startProcess(..))")
	public void startProcessAtProcessManager() { }
	
	public static final String submitProcessAtProcessManager = "com.idega.jbpm.business.BPMPointcuts.submitProcessAtProcessManager()";
	@Pointcut("execution(* com.idega.jbpm.exe.ProcessManager.submitProcess(..))")
	public void submitProcessAtProcessManager() { }
	
	public static final String loadInitViewAtViewManager = "com.idega.jbpm.business.BPMPointcuts.loadInitViewAtViewManager()";
	@Pointcut("execution(* com.idega.jbpm.exe.ViewManager.loadInitView(..))")
	public void loadInitViewAtViewManager() { }
	
	public static final String loadTaskInstanceViewAtViewManager = "com.idega.jbpm.business.BPMPointcuts.loadTaskInstanceViewAtViewManager()";
	@Pointcut("execution(* com.idega.jbpm.exe.ViewManager.loadTaskInstanceView(..))")
	public void loadTaskInstanceViewAtViewManager() { }
}