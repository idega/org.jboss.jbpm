package com.idega.jbpm.def;

import org.jbpm.taskmgmt.def.Task;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:24 $ by $Author: civilis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);
	public abstract View getView(Task task);
	public abstract String getIdentifier();
}