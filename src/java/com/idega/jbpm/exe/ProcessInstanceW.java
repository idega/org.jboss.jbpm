package com.idega.jbpm.exe;

import java.util.Collection;

import org.jbpm.graph.exe.Token;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/06/15 11:56:44 $ by $Author: civilis $
 */
public interface ProcessInstanceW {
	
	//public abstract TaskInstanceW getTaskInstance(long tiId);
	
	public Collection<TaskInstanceW> getAllTaskInstances();
	
	public Collection<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken);
	
	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
}