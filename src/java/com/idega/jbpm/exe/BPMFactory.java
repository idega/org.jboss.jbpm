package com.idega.jbpm.exe;

import java.util.List;

import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.15 $
 * 
 *          Last modified: $Date: 2008/12/09 02:47:47 $ by $Author: civilis $
 */
public interface BPMFactory {

	/**
	 * locates process definition name, and finds process manager Use this only
	 * if you don't have process name
	 * 
	 * @param processDefinitionId
	 * @return Process manager bound to process definition
	 */
	public abstract ProcessManager getProcessManager(long processDefinitionId);

	/**
	 * 
	 * @param processDefinitionId
	 * @return Process manager bound to latest process definition found by name
	 */
	public abstract ProcessManager getProcessManager(String processName);

	/**
	 * 
	 * @param processInstanceId
	 * @return Process manager bound to process definition found by
	 *         processInstanceId
	 */
	public abstract ProcessManager getProcessManagerByProcessInstanceId(
			long processInstanceId);

	/**
	 * 
	 * @param taskInstanceId
	 * @return Process manager bound to process definition found by
	 *         taskInstanceId
	 */
	public abstract ProcessManager getProcessManagerByTaskInstanceId(
			long taskInstanceId);

	/**
	 * use when you don't have neither information (process id, process name and
	 * alike), but you need general methods for processManager (e.g.
	 * getAllProcesses (for this process manager stype))
	 * 
	 * @param managerType
	 * @return
	 */
	public abstract ProcessManager getProcessManagerByType(String managerType);

	/**
	 * Finds viewTaskBind by taskId provided, and finds ViewFactory by preferred
	 * types in order given, if any provided Uses the ViewFactory resolved for
	 * creating View for view identifier resolved in viewTaskBind
	 * 
	 * @param taskId
	 * @param submitable
	 *            - should the view be able to be submitted (e.g. for html
	 *            forms, the submit button could be disabled or hidden)
	 * @param preferredTypes
	 *            - if null, the behavior is the same as calling getView(taskId,
	 *            submitable)
	 * @return
	 */
	public abstract View getViewByTask(long taskId, boolean submitable,
			List<String> preferredTypes);

	public abstract View getViewByTaskInstance(long taskInstanceId,
			boolean submitable, List<String> preferredTypes);

	public abstract View takeView(long taskInstanceId, boolean submitable,
			List<String> preferredTypes);

	public abstract RolesManager getRolesManager();

	public abstract BPMUserFactory getBpmUserFactory();

	/**
	 * returns default view submission implementation
	 * @return
	 */
	public ViewSubmission getViewSubmission();
}