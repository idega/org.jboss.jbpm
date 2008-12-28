package com.idega.jbpm.exe;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.21 $
 * 
 *          Last modified: $Date: 2008/12/28 12:08:04 $ by $Author: civilis $
 */
public interface TaskInstanceW {

	public abstract void submit(ViewSubmission view);

	public abstract void submit(ViewSubmission view, boolean proceedProcess);

	public abstract void start(int userId);

	public abstract void assign(int userId);

	public abstract void assign(User usr);

	public abstract User getAssignedTo();

	/**
	 * loads view for accessing task instance from user interface. Should be
	 * called for displaying task representation in UI.
	 * 
	 * @return
	 */
	public abstract View loadView();

	/**
	 * gets view of task instance The difference between load view, is that load
	 * view may incorporate some additional actions for viewing task in UI.
	 * 
	 * @return
	 */
	public abstract View getView();

	public abstract Long getTaskInstanceId();

	public abstract void setTaskInstanceId(Long taskInstanceId);

	public abstract void setTaskInstance(TaskInstance taskInstance);

	public abstract String getName(Locale locale);

	public abstract TaskInstance getTaskInstance();

	public abstract void setTaskRolePermissions(Role role,
			boolean setSameForAttachments, String variableIdentifier);

	/**
	 * creates, stores and adds binary variable to the variable specified. If
	 * the variable is not found in the process instance (not necessary in the
	 * task instance scope), it is created for the task instance.
	 * 
	 * @param variable
	 * @param fileName
	 * @param description
	 * @param is
	 * @return
	 */
	public abstract BinaryVariable addAttachment(Variable variable,
			String fileName, String description, InputStream is);

	public abstract List<BinaryVariable> getAttachments();

	public abstract BinaryVariable getAttachment(String variableName);

	public boolean isSignable();

	/**
	 * should be used only by process manager itself
	 * 
	 * @param processManager
	 */
	public abstract void setProcessManager(ProcessManager processManager);

	public abstract ProcessInstanceW getProcessInstanceW();
}