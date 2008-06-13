package com.idega.jbpm.exe;

import java.util.Collection;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/06/13 08:13:42 $ by $Author: anton $
 */
public interface ProcessInstanceW {
	
	public abstract TaskInstanceW getTaskInstance(long tiId);
	
	public Collection<TaskInstanceW> getTaskInstances(long processInstanceId, JbpmContext ctx);
	
	public Collection<TaskInstanceW> getUnfinishedTasks(long processInstanceId, Token rootToken , JbpmContext ctx);
	
	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
}