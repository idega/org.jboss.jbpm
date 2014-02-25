package com.idega.jbpm.identity;

import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 *          Last modified: $Date: 2009/02/13 17:27:48 $ by $Author: civilis $
 */
@Service("rightsManagementRolesAssignmentHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RightsManagementRolesAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = -3470015014878632919L;

	@Autowired
	private BPMFactory bpmFactory;
	private String assignmentExpression;

	@Override
	public void execute(ExecutionContext ctx) throws Exception {
		String expression = getAssignmentExpression();
		if (StringUtil.isEmpty(expression))
			return;

		TaskAssignment ta = JSONExpHandler.resolveRightsRolesFromJSONExpression(expression, ctx);
		List<Role> roles = ta.getRoles();
		if (ListUtil.isEmpty(roles))
			return;

		JbpmContext context = ctx.getJbpmContext();
		ProcessInstance pi = getBpmFactory().getMainProcessInstance(context, ctx.getProcessInstance().getId());
		getBpmFactory().getRolesManager().createProcessActors(context, roles, pi);
		getBpmFactory().getRolesManager().assignRolesPermissions(roles, pi);
	}

	public String getAssignmentExpression() {
		return assignmentExpression;
	}

	public void setAssignmentExpression(String assignmentExpression) {
		this.assignmentExpression = assignmentExpression;
	}

	BPMFactory getBpmFactory() {
		if (bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		return bpmFactory;
	}
}