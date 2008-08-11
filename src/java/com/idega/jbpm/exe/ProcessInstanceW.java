package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.Token;



/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/08/11 13:31:17 $ by $Author: civilis $
 */
public interface ProcessInstanceW {
	
	public abstract Collection<TaskInstanceW> getAllTaskInstances();
	
	public abstract Collection<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken);
	
	public abstract List<TaskInstanceW> getAllUnfinishedTaskInstances();
	
	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
	
	public abstract Integer getHandlerId();
	
	/**
	 * if handlerUserId not null - assigns handler to the process,
	 * unassigns otherwise
	 * @param handlerUserId
	 */
	public abstract void assignHandler(Integer handlerUserId);
	
	public abstract ProcessWatch getProcessWatcher();
	
	/**
	 * 
	 * @return if the process can be assigned to the case handler
	 */
	public abstract boolean hasHandlerAssignmentSupport();
}