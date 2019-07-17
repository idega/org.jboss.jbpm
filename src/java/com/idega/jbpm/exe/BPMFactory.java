package com.idega.jbpm.exe;

import java.io.Serializable;
import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.presentation.IWContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $ Last modified: $Date: 2009/03/20 19:17:17 $ by $Author: civilis $
 */
public interface BPMFactory {

	/**
	 * locates process definition name, and finds process manager Use this only if you don't have
	 * process name
	 *
	 * @param processDefinitionId
	 * @return Process manager bound to process definition
	 */
	public abstract <T extends Serializable> ProcessManager getProcessManager(T processDefinitionId);

	/**
	 * @param processDefinitionId
	 * @return Process manager bound to latest process definition found by name
	 */
	public abstract ProcessManager getProcessManager(String processName);

	/**
	 * @param processInstanceId
	 * @return Process manager bound to process definition found by processInstanceId
	 */
	public abstract <T extends Serializable> ProcessManager getProcessManagerByProcessInstanceId(T processInstanceId);

	/**
	 * @param taskInstanceId
	 * @return Process manager bound to process definition found by taskInstanceId
	 */
	public abstract <T extends Serializable> ProcessManager getProcessManagerByTaskInstanceId(T taskInstanceId);

	/**
	 * use when you don't have neither information (process id, process name and alike), but you
	 * need general methods for processManager (e.g. getAllProcesses (for this process managers
	 * type))
	 *
	 * @param managerType
	 * @return
	 */
	public abstract ProcessManager getProcessManagerByType(String managerType);

	/**
	 * Finds viewTaskBind by taskId provided, and finds ViewFactory by preferred types in order
	 * given, if any provided Uses the ViewFactory resolved for creating View for view identifier
	 * resolved in viewTaskBind
	 *
	 * @param taskId
	 * @param submitable
	 *            - should the view be able to be submitted (e.g. for html forms, the submit button
	 *            could be disabled or hidden)
	 * @param preferredTypes
	 *            - if null, the behavior is the same as calling getView(taskId, submitable)
	 * @return
	 */
	public abstract View getViewByTask(long taskId, boolean submitable, List<String> preferredTypes);

	public abstract View getViewByTaskInstance(long taskInstanceId, boolean submitable, List<String> preferredTypes, String... forcedTypes);

	public abstract View getView(String viewIdentifier, String type, boolean submitable);

	public abstract View takeView(long taskInstanceId, boolean submitable, List<String> preferredTypes);

	/**
	 * takes all views that are bound to the task instance task, if not taken already
	 *
	 * @param taskInstanceId
	 */
	public abstract void takeViews(final long taskInstanceId);
	public abstract void takeViews(JbpmContext context, Task task, TaskInstance ti);

	public abstract RolesManager getRolesManager();

	public abstract BPMUserFactory getBpmUserFactory();

	/**
	 * returns default view submission implementation
	 *
	 * @return
	 */
	public abstract ViewSubmission getViewSubmission();

	public abstract BPMDAO getBPMDAO();

	public abstract PermissionsFactory getPermissionsFactory();

	/**
	 * Resolves mainProcessInstance in this manner: if provided processInstance contains variable
	 * "mainProcessInstanceId", then the processInstance from this variable value is used. Else,
	 * super process instances are checked for this variable. If the variable is not found in super
	 * processes, then the most super process instance is used - that means the process instance
	 * without the parent
	 *
	 * @param processInstanceId
	 *            of any process instance (subprocess or not)
	 * @return the main process instance, which reflects the most parent process instance. This
	 *         should be used for all assignments (creating actors)
	 */
	public abstract ProcessInstance getMainProcessInstance(final long processInstanceId);

	public abstract ProcessInstance getMainProcessInstance(JbpmContext context, final long processInstanceId);

	public abstract <T extends Serializable> TaskInstanceW getTaskInstanceW(T taskInstanceId);

	public abstract <T extends Serializable> ProcessInstanceW getProcessInstanceW(T processInstanceId);
	public abstract <T extends Serializable> ProcessInstanceW getProcessInstanceW(JbpmContext context, T processInstanceId);

	public abstract ProcessDefinitionW getProcessDefinitionW(String processName);

	public <T extends Serializable> T getIdOfStartTaskInstance(T piId, IWContext iwc);

	public <T extends Serializable> T getVariable(ExecutionContext ctx, String name);
	public <T extends Serializable> T getVariable(ExecutionContext ctx, String name, Long piId);

}