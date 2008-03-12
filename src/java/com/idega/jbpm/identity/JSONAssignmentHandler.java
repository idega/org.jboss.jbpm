package com.idega.jbpm.identity;

import java.util.List;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.identity.assignment.ExpressionAssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.webface.WFUtil;

/**
 * <p>Expects assignment expression in json notation. E.g.:</p>
 * 
 * <p>
 * <code>
 * {taskAssignment: {roles: {role: [
 *	{roleName: handler, accesses: {access: [read, write]}},
 *	{roleName: owner, accesses: {access: [read, write]}} 
 * ]} }}
 * </code>
 * </p>
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 * 
 * Last modified: $Date: 2008/03/12 15:43:02 $ by $Author: civilis $
 */
public class JSONAssignmentHandler extends ExpressionAssignmentHandler {
	
	private static final long serialVersionUID = 8955094455268141204L;
	
	private static final String rolesAssignerBeanIdentifier = "bpmRolesAssiger";

	public void assign(Assignable assignable, ExecutionContext executionContext) {

		if(!(assignable instanceof TaskInstance))
			throw new IllegalArgumentException("Only TaskInstance is accepted for assignable");
		
		TaskInstance taskInstance = (TaskInstance)assignable;
		
		List<Role> roles = JSONExpHandler.resolveRolesFromJSONExpression(expression);
		
		RolesAssiger rolesAssigner = getRolesAssigner();
		rolesAssigner.assign(taskInstance, roles);
	}
	
	protected RolesAssiger getRolesAssigner() {
		
		return (RolesAssiger)WFUtil.getBeanInstance(rolesAssignerBeanIdentifier);
	}
}