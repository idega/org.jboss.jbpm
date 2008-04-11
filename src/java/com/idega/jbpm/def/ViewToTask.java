package com.idega.jbpm.def;

import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 * 
 * Last modified: $Date: 2008/04/11 01:28:24 $ by $Author: civilis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);
	
	public abstract void bind(View view, TaskInstance taskInstance);

	public abstract Long getTask(String viewId);

	public abstract void unbind(String viewId);
}