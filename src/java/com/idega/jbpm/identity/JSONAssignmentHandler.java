package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.identity.assignment.ExpressionAssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.VariablesResolver;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * <p>
 * Expects assignment expression in json notation. E.g.:
 * </p>
 * <p>
 * <code>
 * {taskAssignment: {roles: {role: [
 * 	{roleName: handler, accesses: {access: [read, write]}},
 * 	{roleName: owner, accesses: {access: [read, write]}}
 * ]} }}
 * </code>
 * </p>
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.19 $ Last modified: $Date: 2009/03/19 15:41:24 $ by $Author: juozas $
 *
 *          Last modified: $Date: 2009/03/19 15:41:24 $ by $Author: juozas $
 */
@Service("jsonAssignmentHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class JSONAssignmentHandler extends ExpressionAssignmentHandler {

	private static final String ASSIGN_IDENTITY_CURRENT_USER = "current_user";

	private static final long serialVersionUID = 8955094455268141204L;

	private static final Logger LOGGER = Logger.getLogger(JSONAssignmentHandler.class.getName());

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private VariablesResolver variablesResolver;


	@Override
	public void assign(Assignable assignable, ExecutionContext executionContext) {
		if (!(assignable instanceof TaskInstance)) {
			throw new IllegalArgumentException("Only TaskInstance is accepted for assignable");
		}

		this.executionContext = executionContext;
		String exp = getExpression();
		if (StringUtil.isEmpty(exp)) {
			return;
		}

		TaskInstance taskInstance = (TaskInstance) assignable;
		long tiId = taskInstance.getId();
		taskInstance = executionContext.getJbpmContext().getTaskInstance(tiId);
		if (taskInstance == null) {
			throw new IllegalArgumentException("TaskInstance (ID: " + tiId + ") is not initialized!");
		}

		long piId = taskInstance.getProcessInstance().getId();
		ProcessManager pm = null;
		try {
			pm = getBpmFactory().getProcessManager(executionContext.getProcessDefinition().getName());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting process manager by proc. def. name. Task inst. ID: " + tiId, e);
		}
		if (pm == null) {
			try {
				pm = getBpmFactory().getProcessManagerByTaskInstanceId(tiId);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting process manager by task inst. ID: " + tiId, e);
			}
		}
		if (pm == null) {
			throw new RuntimeException("Process manager can not be loaded. Task inst. ID: " + tiId);
		}
		TaskInstanceW tiw = pm.getTaskInstance(tiId);

		TaskAssignment ta = JSONExpHandler.resolveRolesFromJSONExpression(exp, executionContext);
		if (ta == null) {
			LOGGER.warning(TaskAssignment.class.getName() + " was not resolved from expression:\n" + exp);
			return;
		}

		List<Role> roles = ta.getRoles();
		if (ListUtil.isEmpty(roles)) {
			LOGGER.warning("No roles for task instance: " + taskInstance.getId());
			return;
		}

		JbpmContext context = executionContext.getJbpmContext();
		ProcessInstance mainProcessInstance = getBpmFactory().getMainProcessInstance(context, piId);

		// Filtering roles
		List<Role> rolesForTask = new ArrayList<Role>();
		List<Role> rolesForTaskInstance = new ArrayList<Role>();
		List<Role> userRolesForTask = new ArrayList<Role>();
		List<Role> userRolesForTaskInstance = new ArrayList<Role>();

		for (Role role : roles) {
			if (role.getUserId() != null) {
				if (role.getForTaskInstance()) {
					userRolesForTaskInstance.add(role);
				} else {
					roles.remove(role);
					userRolesForTask.add(role);
				}
			} else if (role.getForTaskInstance() && role.getUserId() == null) {
				rolesForTaskInstance.add(role);
			} else {
				rolesForTask.add(role);
			}
		}

		// Roles that are for task
		getBpmFactory().getRolesManager().createProcessActors(context, rolesForTask, mainProcessInstance);
		getBpmFactory().getRolesManager().assignTaskRolesPermissions(context, taskInstance.getTask().getId(), rolesForTask, mainProcessInstance.getId());
		assignIdentities(mainProcessInstance, rolesForTask);

		// Roles for task instance
		getBpmFactory().getRolesManager().createProcessActors(context, rolesForTaskInstance, taskInstance.getProcessInstance());
		for (Role role : rolesForTaskInstance) {
			tiw.setTaskRolePermissions(role, true, null);
		}
		assignIdentities(mainProcessInstance, rolesForTaskInstance);

		// User "roles" for task
		for (Role role : userRolesForTask) {
			List<Actor> processActors = getBpmFactory().getRolesManager().createProcessActors(context, Arrays.asList(role), mainProcessInstance);
			getBpmFactory().getRolesManager().assignTaskRolesPermissions(context, taskInstance.getTask().getId(), userRolesForTask,
					mainProcessInstance.getId());

			getBpmFactory().getRolesManager().createIdentitiesForActors(
				processActors, new Identity(String.valueOf(role.getUserId()), IdentityType.USER),
				taskInstance.getProcessInstance().getId());
		}


		// User "roles" for task instance
		for (Role role : userRolesForTaskInstance) {
			List<Actor> processActors = getBpmFactory().getRolesManager().createProcessActors(context, Arrays.asList(role),
					taskInstance.getProcessInstance());

			getBpmFactory().getRolesManager().createIdentitiesForActors(
			    processActors, new Identity(String.valueOf(role.getUserId()), IdentityType.USER),
			    taskInstance.getProcessInstance().getId());

			tiw.setTaskPermissionsForActors(processActors, role.getAccesses(), true, null);
		}
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getExpression() {
		if (expression == null) {
			return null;
		}

		expression = JSONExpHandler.getResolvedExpression(expression, executionContext);

		return expression;
	}

	private void assignIdentities(ProcessInstance processInstance, List<Role> roles) {
		BPMUser usr = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
		if (usr == null)
			return;

		Integer usrId = usr.getIdToUse();
		if (usrId == null)
			return;

		List<Role> rolesToAssignIdentity = new ArrayList<Role>(roles.size());
		for (Role role : roles) {
			if (role.getAssignIdentities() != null) {
				for (String assignTo: role.getAssignIdentities()) {
					if (assignTo.equals(ASSIGN_IDENTITY_CURRENT_USER)) {
						rolesToAssignIdentity.add(role);
						break;
					}
				}
			}
		}

		if (!rolesToAssignIdentity.isEmpty()) {
			getBpmFactory().getRolesManager().createIdentitiesForRoles(
			    rolesToAssignIdentity,
			    new Identity(String.valueOf(usrId), IdentityType.USER),
			    processInstance.getId());
		}
	}

	BPMFactory getBpmFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmFactory;
	}

	public VariablesResolver getVariablesResolver() {
    	return variablesResolver;
    }

	public void setVariablesResolver(VariablesResolver variablesResolver) {
    	this.variablesResolver = variablesResolver;
    }
}