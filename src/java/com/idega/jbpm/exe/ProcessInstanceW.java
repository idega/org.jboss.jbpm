package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;

import org.jbpm.graph.exe.Token;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/06/15 16:33:02 $ by $Author: civilis $
 */
public interface ProcessInstanceW {
	
	public abstract Collection<TaskInstanceW> getAllTaskInstances();
	
	public abstract Collection<TaskInstanceW> getUnfinishedTaskInstances(Token rootToken);
	
	public abstract List<TaskInstanceW> getAllUnfinishedTaskInstances();
	
	public abstract Long getProcessInstanceId();
	
	public abstract void setProcessInstanceId(Long processInstanceId);
}