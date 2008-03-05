package com.idega.jbpm.identity;

import java.util.List;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.identity.assignment.ExpressionAssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

import com.idega.jbpm.identity.RolesAssiger.Access;
import com.idega.webface.WFUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

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
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
 */
public class JSONAssignmentHandler extends ExpressionAssignmentHandler {
	
	private static final long serialVersionUID = 8955094455268141204L;
	
	private static final String rolesAssignerBeanIdentifier = "bpmRolesAssiger";
	private static final String taskAssignment = "taskAssignment";
	private static final String role = "role";
	private static final String access = "access";
	
	private class TaskAssignment { List<Role> roles; }

	public void assign(Assignable assignable, ExecutionContext executionContext) {

		System.out.println("assigning w/ json");
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(taskAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);
		
		TaskAssignment assignmentExp = (TaskAssignment)xstream.fromXML(expression);

		List<Role> roles = assignmentExp.roles;
		
		RolesAssiger rolesAssigner = getRolesAssigner();
		rolesAssigner.assign(assignable, roles);
	}
	
	protected RolesAssiger getRolesAssigner() {
		
		return (RolesAssiger)WFUtil.getBeanInstance(rolesAssignerBeanIdentifier);
	}
}