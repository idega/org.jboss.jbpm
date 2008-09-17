package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/09/17 18:17:32 $ by $Author: civilis $
 */
public interface ProcessManager {
	
	public abstract ProcessDefinitionW getProcessDefinition(long pdId);
	
	/**
	 * @param processName
	 * @return wrapper of latest process definition found by name
	 */
	public abstract ProcessDefinitionW getProcessDefinition(String processName);
	
	public abstract ProcessInstanceW getProcessInstance(long piId);
	
	public abstract TaskInstanceW getTaskInstance(long tiId);
}