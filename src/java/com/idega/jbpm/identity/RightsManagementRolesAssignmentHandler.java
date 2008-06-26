package com.idega.jbpm.identity;

import java.util.List;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.presentation.IWContext;
import com.idega.util.expression.ELUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/06/26 15:33:33 $ by $Author: anton $
 */
public class RightsManagementRolesAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -3470015014878632919L;
	private static final String rolesAssignerBeanIdentifier = "bpmRolesAssiger";
	
	private String assignmentExpression;
	
	public void execute(ExecutionContext ctx) throws Exception {
		System.out.println(getAssignmentExpression());

		if(getAssignmentExpression() != null) {
			List<Role> roles = JSONExpHandler.resolveRightsRolesFromJSONExpression(getAssignmentExpression());
			
			FacesContext fctx = FacesContext.getCurrentInstance();
			final IWContext iwc = IWContext.getIWContext(fctx);
			
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