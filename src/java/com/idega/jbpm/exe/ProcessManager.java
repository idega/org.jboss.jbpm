package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/09/02 12:59:47 $ by $Author: civilis $
 */
public interface ProcessManager {
	
	public abstract ProcessDefinitionW getProcessDefinition(long pdId);
	
	public abstract ProcessInstanceW getProcessInstance(long piId);
	
	public abstract TaskInstanceW getTaskInstance(long tiId);
}