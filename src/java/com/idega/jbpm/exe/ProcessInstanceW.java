package com.idega.jbpm.exe;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/05 12:17:03 $ by $Author: civilis $
 */
public interface ProcessInstanceW {
	
	public abstract TaskInstanceW getTaskInstance(long tiId);
	
	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
}