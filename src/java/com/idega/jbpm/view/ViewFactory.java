package com.idega.jbpm.view;

import java.util.Collection;

import org.jbpm.taskmgmt.def.Task;

import com.google.common.collect.Multimap;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2009/01/26 15:11:40 $ by $Author: civilis $
 */
public interface ViewFactory {

	public abstract String getViewType();
	
	public abstract View getView(String viewIdentifier, boolean submitable);
	
	public abstract TaskView getTaskView(Task task);
	
	public abstract Multimap<Long, TaskView> getAllViewsByProcessDefinitions(Collection<Long> processDefinitionsIds);
}