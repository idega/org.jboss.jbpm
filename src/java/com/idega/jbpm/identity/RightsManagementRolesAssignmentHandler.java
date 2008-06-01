package com.idega.jbpm.identity;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/06/01 12:02:38 $ by $Author: civilis $
 */
public class RightsManagementRolesAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -3470015014878632919L;
	private String assignmentExpression;
	
	public void execute(ExecutionContext ctx) throws Exception {

		//System.out.println("_______________expression: "+getAssignmentExpression());

//		{rightsAssignment: {roles: {role: [
//		                                   {roleName: "bpm_handler", accesses: {access: [modifyPermissions]}}
//		                               ]} }}
		
	}

	public String getAssignmentExpression() {
		return assignmentExpression;
	}

	public void setAssignmentExpression(String assignmentExpression) {
		this.assignmentExpression = assignmentExpression;
	}
}