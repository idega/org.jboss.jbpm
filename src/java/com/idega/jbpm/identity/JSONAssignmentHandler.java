package com.idega.jbpm.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.identity.assignment.ExpressionAssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.exe.BPMFactory;

/**
 * <p>
 * Expects assignment expression in json notation. E.g.:
 * </p>
 * 
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
 * @version $Revision: 1.17 $
 * 
 *          Last modified: $Date: 2009/02/13 17:27:48 $ by $Author: civilis $
 */
@Service("jsonAssignmentHandler")
@Scope("prototype")
public class JSONAssignmentHandler extends ExpressionAssignmentHandler {

	private static final String ASSIGN_IDENTITY_CURRENT_USER = "current_user";

	private static final long serialVersionUID = 8955094455268141204L;
	@Autowired
	private BPMFactory bpmFactory;

	public void assign(Assignable assignable, ExecutionContext executionContext) {

		if (!(assignable instanceof TaskInstance))
			throw new IllegalArgumentException(
					"Only TaskInstance is accepted for assignable");

		this.executionContext = executionContext;

		String exp = getExpression();

		if (exp != null) {

			TaskInstance taskInstance = (TaskInstance) assignable;

			TaskAssignment ta = JSONExpHandler
					.resolveRolesFromJSONExpression(exp);
			
			List<Role> roles = ta.getRoles();

			if (roles == null || roles.isEmpty()) {

				Logger.getLogger(getClass().getName()).log(Level.WARNING,
						"No roles for task instance: " + taskInstance.getId());
				return;
			}
			
			ProcessInstance mainProcessInstance = getBpmFactory().getMainProcessInstance(taskInstance.getProcessInstance().getId());
			
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
			
			getBpmFactory().getRolesManager().createProcessActors(roles, mainProcessInstance);
			getBpmFactory().getRolesManager().assignTaskRolesPermissions(
					taskInstance.getTask(), roles, mainProcessInstance.getId());
			assignIdentities(mainProcessInstance, roles);
		}
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getExpression() {
		return expression;
	}

	private void assignIdentities(ProcessInstance processInstance, List<Role> roles) {

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
							rolesToAssignIdentity, String.valueOf(usrId),
							IdentityType.USER,
							processInstance.getId());
				}
			}
		}
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
}