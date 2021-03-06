package com.idega.jbpm.exe;

import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $
 * 
 *          Last modified: $Date: 2009/01/25 15:36:31 $ by $Author: civilis $
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

	public abstract TaskInstanceW getTaskInstance(TaskInstance ti);

	public abstract List<ProcessDefinitionW> getAllProcesses();

	public abstract String getManagerType();
}