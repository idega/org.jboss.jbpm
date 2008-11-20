package com.idega.jbpm.exe;

import java.util.List;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/11/20 07:30:43 $ by $Author: valdas $
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

	public abstract List<ProcessDefinitionW> getAllProcesses();
}