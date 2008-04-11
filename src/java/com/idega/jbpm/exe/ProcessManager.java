package com.idega.jbpm.exe;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/04/11 01:28:24 $ by $Author: civilis $
 */
public interface ProcessManager {
	
	public abstract void startProcess(long startTaskInstanceId, View view);
	
	public abstract void submitTaskInstance(long taskInstanceId, View view);
	
	public abstract void startTask(long taskInstanceId, int userId);
	
	public abstract void assignTask(long taskInstanceId, int userId);
}