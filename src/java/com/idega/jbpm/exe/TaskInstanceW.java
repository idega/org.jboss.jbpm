package com.idega.jbpm.exe;

import java.io.InputStream;
import java.util.Locale;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.view.View;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.15 $
 * 
 *          Last modified: $Date: 2008/12/02 13:34:16 $ by $Author: civilis $
 */
public interface TaskInstanceW {

	public abstract void submit(View view);

	public abstract void submit(View view, boolean proceedProcess);

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
}