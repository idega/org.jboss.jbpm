package com.idega.jbpm.identity;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 * 
 *          Last modified: $Date: 2008/11/26 16:30:26 $ by $Author: civilis $
 */
@Service("rightsManagementRolesAssignmentHandler")
@Scope("prototype")
public class RightsManagementRolesAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -3470015014878632919L;

	@Autowired
	private BPMFactory bpmFactory;
	private String assignmentExpression;

	public void execute(ExecutionContext ctx) throws Exception {
		if (getAssignmentExpression() != null) {
			List<Role> roles = JSONExpHandler
					.resolveRightsRolesFromJSONExpression(getAssignmentExpression());

			if (roles != null && !roles.isEmpty()) {

				getBpmFactory().getRolesManager().createProcessActors(roles,
						ctx.getProcessInstance());
				getBpmFactory().getRolesManager().assignRolesPermissions(roles,
						ctx.getProcessInstance());
			}
		}
	}

	public String getAssignmentExpression() {
		return assignmentExpression;
	}

	public void setAssignmentExpression(String assignmentExpression) {
		this.assignmentExpression = assignmentExpression;
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
}