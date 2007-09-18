package com.idega.jbpm.def;

import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/09/18 09:45:39 $ by $Author: civilis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);
	public abstract View getView(long taskId);
	public abstract String getIdentifier();
}