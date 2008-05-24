package com.idega.jbpm.exe;

import java.util.List;

import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.view.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2008/05/24 10:25:52 $ by $Author: civilis $
 */
public interface BPMFactory {

	/**
	 * 
	 * @param processDefinitionId
	 * @return View manager bound to process definition
	 */
	public abstract ProcessManager getProcessManager(long processDefinitionId);
	
	/**
	 * 
	 * @param taskInstanceId
	 * @return View manager bound to process definition found by taskInstanceId
	 */
	public abstract ProcessManager getProcessManagerByTaskInstanceId(long taskInstanceId);
	
	/**
	 * Finds viewTaskBind by taskId provided, and finds ViewFactory by preferred types in order given, if any provided
	 * Uses the ViewFactory resolved for creating View for view identifier resolved in viewTaskBind
	 * @param taskId
	 * @param submitable - should the view be able to be submitted (e.g. for html forms, the submit button could be disabled or hidden)
	 * @param preferredTypes - if null, the behavior is the same as calling getView(taskId, submitable)
	 * @return
	 */
	public abstract View getViewByTask(long taskId, boolean submitable, List<String> preferredTypes);
	
	public abstract View getViewByTaskInstance(long taskInstanceId, boolean submitable, List<String> preferredTypes);
	
	public abstract View takeView(long taskInstanceId, boolean submitable, List<String> preferredTypes);
	
	public abstract RolesManager getRolesManager();
	
	public abstract BPMUserFactory getBpmUserFactory();
}