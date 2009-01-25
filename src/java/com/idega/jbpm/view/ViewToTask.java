package com.idega.jbpm.view;

import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 *          Last modified: $Date: 2009/01/25 15:36:31 $ by $Author: civilis $
 */
public interface ViewToTask {

	/**
	 * binds view to task using view id and viewType
	 * 
	 * @param viewId
	 * @param viewType
	 * @param task
	 */
	public abstract void bind(String viewId, String viewType, Task task);

	/**
	 * the same as bind(String viewId, String viewType, Task task), but viewId
	 * and viewType are resolved from view (convenience method)
	 * 
	 * @param view
	 * @param task
	 */
	public abstract void bind(View view, Task task);

	/**
	 * @see bind(View view, Task task), but binds with task instance
	 * @param view
	 * @param taskInstance
	 */
	public abstract void bind(View view, TaskInstance taskInstance);
}