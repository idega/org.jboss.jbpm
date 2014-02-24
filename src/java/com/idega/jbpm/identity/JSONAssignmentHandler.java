package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.variables.VariablesResolver;

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

	@Autowired
	private BPMFactory bpmFactory;

	@Autowired
	private VariablesResolver variablesResolver;


	@Override
	public void assign(Assignable assignable, ExecutionContext executionContext) {

		if (!(assignable instanceof TaskInstance))
			throw new IllegalArgumentException(
			        "Only TaskInstance is accepted for assignable");

		this.executionContext = executionContext;

		String exp = getExpression();

		if (exp != null) {

			TaskInstance taskInstance = (TaskInstance) assignable;

			TaskInstanceW tiw = getBpmFactory()
			        .getProcessManagerByProcessInstanceId(
			            taskInstance.getProcessInstance().getId())
			        .getTaskInstance(taskInstance.getId());


			TaskAssignment ta = JSONExpHandler
			        .resolveRolesFromJSONExpression(exp, executionContext);

			List<Role> roles = ta.getRoles();

			if (roles == null || roles.isEmpty()) {

				Logger.getLogger(getClass().getName()).log(Level.WARNING,
				    "No roles for task instance: " + taskInstance.getId());
				return;
			}


			ProcessInstance mainProcessInstance = getBpmFactory()
			        .getMainProcessInstance(
			            taskInstance.getProcessInstance().getId());

			/*
			if(mainProcessInstance == null) {
//				backwards compatibility

				if (ta.getRolesFromProcessInstanceId() != null) {

					mainProcessInstance = executionContext.getJbpmContext().getProcessInstance(
							ta.getRolesFromProcessInstanceId());
				} else {
					mainProcessInstance = taskInstance.getProcessInstance();
				}
			}
			*/
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
				}else{
					rolesForTask.add(role);
				}
			}

			// Roles that are for task
			getBpmFactory().getRolesManager().createProcessActors(rolesForTask, mainProcessInstance);
			getBpmFactory().getRolesManager().assignTaskRolesPermissions(
			    taskInstance.getTask(), rolesForTask, mainProcessInstance.getId());
			assignIdentities(mainProcessInstance, rolesForTask);

			// Roles for task instance
			getBpmFactory().getRolesManager().createProcessActors(
			    rolesForTaskInstance, taskInstance.getProcessInstance());

			for (Role role : rolesForTaskInstance) {
				tiw.setTaskRolePermissions(role, true, null);
			}
			assignIdentities(mainProcessInstance, rolesForTaskInstance);

			// User "roles" for task
			for (Role role : userRolesForTask) {
				List<Actor> processActors = getBpmFactory().getRolesManager()
				        .createProcessActors(Arrays.asList(role), mainProcessInstance);

				getBpmFactory().getRolesManager().assignTaskRolesPermissions(
				    taskInstance.getTask(), userRolesForTask, mainProcessInstance.getId());

				getBpmFactory().getRolesManager().createIdentitiesForActors(
				    processActors, new Identity(String.valueOf(role.getUserId()), IdentityType.USER),
				    taskInstance.getProcessInstance().getId());
			}


			// User "roles" for task instance
			for (Role role : userRolesForTaskInstance) {
				List<Actor> processActors = getBpmFactory().getRolesManager()
				        .createProcessActors(Arrays.asList(role),
				            taskInstance.getProcessInstance());

				getBpmFactory().getRolesManager().createIdentitiesForActors(
				    processActors, new Identity(String.valueOf(role.getUserId()), IdentityType.USER),
				    taskInstance.getProcessInstance().getId());

				tiw.setTaskPermissionsForActors(processActors, role
				        .getAccesses(), true, null);
			}



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

	private void assignIdentities(ProcessInstance processInstance,
	        List<Role> roles) {

		BPMUser usr = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();

		if (usr != null) {

			Integer usrId = usr.getIdToUse();

			if (usrId != null) {

				ArrayList<Role> rolesToAssignIdentity = new ArrayList<Role>(
				        roles.size());

				for (Role role : roles) {

					if (role.getAssignIdentities() != null) {

						for (String assignTo : role.getAssignIdentities()) {

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
		}
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public VariablesResolver getVariablesResolver() {
    	return variablesResolver;
    }

	public void setVariablesResolver(VariablesResolver variablesResolver) {
    	this.variablesResolver = variablesResolver;
    }
}