package com.idega.jbpm.def;

import java.util.Collection;

import org.jbpm.taskmgmt.def.Task;

import com.google.common.collect.Multimap;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 * 
 * Last modified: $Date: 2008/04/10 01:20:26 $ by $Author: civilis $
 */
public interface ViewToTask {

	public abstract void bind(View view, Task task);

	public abstract View getView(long taskId);

	public abstract Long getTask(String formId);

	public abstract void unbind(String viewId);

	public abstract Multimap<Long, TaskView> getAllViewsByProcessDefinitions(
			Collection<Long> processDefinitionsIds);
}