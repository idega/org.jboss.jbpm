package com.idega.jbpm.identity;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.expression.ELUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/11/13 15:08:32 $ by $Author: juozas $
 */
@Service("rightsManagementRolesAssignmentHandler")
@Scope("prototype")
public class RightsManagementRolesAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -3470015014878632919L;
	private static final String rolesAssignerBeanIdentifier = "bpmRolesAssiger";
	
	private String assignmentExpression;
	
	public void execute(ExecutionContext ctx) throws Exception {
		if(getAssignmentExpression() != null) {
			List<Role> roles = JSONExpHandler.resolveRightsRolesFromJSONExpression(getAssignmentExpression());
			
			RolesAssiger rolesAssigner = getRolesAssigner();
			rolesAssigner.assign(ctx.getProcessInstance(), roles);
			rolesAssigner.createRolesPermissions(ctx.getProcessInstance(), roles);
		}
	}

	public String getAssignmentExpression() {
		return assignmentExpression;
	}

	public void setAssignmentExpression(String assignmentExpression) {
		this.assignmentExpression = assignmentExpression;
	}
	
	protected RolesAssiger getRolesAssigner() {
		return ELUtil.getInstance().getBean(rolesAssignerBeanIdentifier);
	}
}