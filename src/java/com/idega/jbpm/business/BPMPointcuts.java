package com.idega.jbpm.business;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/04 18:12:27 $ by $Author: civilis $
 */
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
}