package com.idega.jbpm.exe;

import java.util.List;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/02/25 16:17:28 $ by $Author: civilis $
 */
public interface BPMFactory {

	/**
	 * 
	 * @param processDefinitionId
	 * @return View manager bound to process definition
	 */
	public abstract ViewManager getViewManager(long processDefinitionId);
	
	/**
	 * 
	 * @param processDefinitionId
	 * @return View manager bound to process definition
	 */
	public abstract ProcessManager getProcessManager(long processDefinitionId);
	
	/**
	 * @see javadoc for getView(long taskId, boolean submitable, List<String> preferredTypes);
	 * @param taskId
	 * @param submitable
	 * @return
	 */
	public abstract View getView(long taskId, boolean submitable);
	
	/**
	 * Finds viewTaskBind by taskId provided, and finds ViewFactory by preferred types in order given, if any provided
	 * Uses the ViewFactory resolved for creating View for view identifier resolved in viewTaskBind
	 * @param taskId
	 * @param submitable - should the view be able to be submitted (e.g. for html forms, the submit button could be disabled or hidden)
	 * @param preferredTypes - if null, the behavior is the same as calling getView(taskId, submitable)
	 * @return
	 */
	public abstract View getView(long taskId, boolean submitable, List<String> preferredTypes);
}