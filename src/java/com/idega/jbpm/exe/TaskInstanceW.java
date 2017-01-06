package com.idega.jbpm.exe;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.permission.Access;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.29 $ Last modified: $Date: 2009/05/05 09:04:31 $ by $Author: civilis $
 */
public interface TaskInstanceW {

	public abstract void submit(ViewSubmission view);
	public abstract void submit(JbpmContext context, ViewSubmission view);
	public abstract void submit(ViewSubmission view, boolean proceedProcess);

	public abstract void start(int userId);

	public abstract void assign(int userId);

	public abstract void assign(User usr);

	public abstract User getAssignedTo();

	/**
	 * loads view for accessing task instance from user interface. Should be called for displaying
	 * task representation in UI.
	 *
	 * @return
	 */
	public abstract View loadView();
	public abstract View loadView(String viewType);

	/**
	 * gets view of task instance The difference between load view, is that load view may
	 * incorporate some additional actions for viewing task in UI.
	 *
	 * @return
	 */
	View getView();

	public abstract Long getTaskInstanceId();

	public abstract void setTaskInstanceId(Long taskInstanceId);

	public abstract void setTaskInstance(TaskInstance taskInstance);

	public abstract String getName(Locale locale);

	public abstract TaskInstance getTaskInstance();
	public abstract TaskInstance getTaskInstance(JbpmContext context);

	public abstract void setTaskRolePermissions(Role role, boolean setSameForAttachments, String variableIdentifier);

	/**
	 * creates, stores and adds binary variable to the variable specified. If the variable is not
	 * found in the process instance (not necessary in the task instance scope), it is created for
	 * the task instance.
	 *
	 * @param variable
	 * @param fileName
	 * @param description
	 * @param is
	 * @return
	 */
	public abstract BinaryVariable addAttachment(Variable variable, String fileName, String description, InputStream is);
	public abstract BinaryVariable addAttachment(
			Variable variable,
			String fileName,
			String description,
			InputStream is,
			String destinationInRepository
	);
	public abstract BinaryVariable addAttachment(
			Variable variable,
			String fileName,
			String description,
			InputStream is,
			String destinationInRepository,
			boolean overwrite
	);
	public BinaryVariable addAttachment(Variable variable, String fileName, String description, String pathInRepository);

	public boolean hasAttachment(String identifier, String variableName);

	public abstract List<BinaryVariable> getAttachments();

	public abstract List<BinaryVariable> getAttachments(Variable variable);

	public boolean isSignable();

	/**
	 *
	 * <p>Checks if current task is submitted.</p>
	 * @return <code>true</code> if submitted, <code>false</code>
	 * otherwise.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public boolean isSubmitted();

	/**
	 * should be used only by process manager itself
	 *
	 * @param processManager
	 */
	public abstract void setProcessManager(ProcessManager processManager);

	public abstract ProcessInstanceW getProcessInstanceW(JbpmContext context);
	public abstract ProcessInstanceW getProcessInstanceW();

	public void setTaskPermissionsForActors(
	        List<Actor> actorsToSetPermissionsTo,
	        List<Access> accesses,
	        boolean setSameForAttachments,
	        String variableIdentifier
	);

	public abstract Collection<Role> getRolesPermissions();

	public abstract Collection<Role> getAttachmentRolesPermissions(String attachmentHashValue);

	public abstract Object getVariable(String variableName);

	public abstract void hide();

	public abstract boolean addVariable(Variable variable, Object value);

	public Integer getOrder();

	public Map<String, Object> getVariables(Token token);

	public String getPDFName(Locale locale);

	public boolean isRenderable();

	public boolean hasViewForDisplay();

	public abstract Date getEnd();

	public abstract Date getStart();

	public abstract Date getCreate();

	public abstract String getActorId();

}