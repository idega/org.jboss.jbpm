package com.idega.jbpm.def;

import java.util.Collection;

import org.jbpm.taskmgmt.def.Task;

import com.google.common.collect.Multimap;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2008/04/11 01:28:24 $ by $Author: civilis $
 */
public interface ViewFactory {

	public abstract String getViewType();
	
	public abstract View getView(String viewIdentifier, boolean submitable);
	
	public abstract TaskView getTaskView(Task task);
	
	public abstract String getBeanIdentifier();
	
	public abstract Multimap<Long, TaskView> getAllViewsByProcessDefinitions(Collection<Long> processDefinitionsIds);
}