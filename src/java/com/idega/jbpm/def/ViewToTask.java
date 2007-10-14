package com.idega.jbpm.def;

import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2007/10/14 10:55:08 $ by $Author: civilis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);
	public abstract View getView(long taskId);
}