package com.idega.jbpm.def;

import java.util.List;

import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2007/10/26 12:35:59 $ by $Author: alexis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);
	public abstract View getView(long taskId);
	public List<View> getAllViewsForViewType(String viewType);
}