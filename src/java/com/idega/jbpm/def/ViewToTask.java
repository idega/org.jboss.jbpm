package com.idega.jbpm.def;

import java.util.List;

import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2007/11/10 14:02:06 $ by $Author: alexis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);
	public abstract View getView(long taskId);
	public abstract List<View> getAllViewsForViewType(String viewType);
	public abstract Long getTask(String formId);
}