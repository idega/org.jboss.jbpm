package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/05/04 18:12:26 $ by $Author: civilis $
 */
public interface ProcessManager {
	
	public abstract ProcessDefinitionW getProcessDefinition(long pdId);
	
	public abstract ProcessInstanceW getProcessInstance(long piId);
	
	public abstract TaskInstanceW getTaskInstance(long tiId);
	
//	public abstract void startProcess(long startTaskInstanceId, View view);
//	
//	public abstract void submitTaskInstance(long taskInstanceId, View view);
//	
//	public abstract void submitTaskInstance(long taskInstanceId, View view, boolean proceedProcess);
//	
//	public abstract void startTask(long taskInstanceId, int userId);
//	
//	public abstract void assignTask(long taskInstanceId, int userId);
}