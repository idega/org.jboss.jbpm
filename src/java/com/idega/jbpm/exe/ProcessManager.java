package com.idega.jbpm.exe;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/13 17:00:38 $ by $Author: civilis $
 */
public interface ProcessManager {
	
	public abstract void startProcess(long processDefinitionId, View view);
	
	public abstract void submitTaskInstance(long taskInstanceId, View view);
	
	public abstract void startTask(long taskInstanceId, int userId);
	
	public abstract void assignTask(long taskInstanceId, int userId);
}