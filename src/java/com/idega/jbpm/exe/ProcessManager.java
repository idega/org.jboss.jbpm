package com.idega.jbpm.exe;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/25 15:24:25 $ by $Author: civilis $
 */
public interface ProcessManager {
	
	public abstract void startProcess(long processDefinitionId, View view);
	
	public abstract void submitTaskInstance(long taskInstanceId, View view);
}