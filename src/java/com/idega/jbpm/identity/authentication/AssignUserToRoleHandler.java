package com.idega.jbpm.identity.authentication;

import java.util.ArrayList;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.util.expression.ELUtil;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/06/12 18:29:53 $ by $Author: civilis $
 */
public class AssignUserToRoleHandler implements ActionHandler {

	private static final long serialVersionUID = 2953390756074619221L;
	private RolesManager rolesManager;
	private String userIdExp;
	private String roleExpressionExp;
	private String processInstanceIdExp;
	
	public void execute(ExecutionContext ectx) throws Exception {

		if(getUserIdExp() != null && getProcessInstanceIdExp() != null && getRoleExpressionExp() != null) {

			ELUtil.getInstance().autowire(this);
			
			Integer userId = 	(Integer)JbpmExpressionEvaluator.evaluate(getUserIdExp(), ectx);
			
			Long pid = 			(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
			ProcessInstance pi = ectx.getJbpmContext().getProcessInstance(pid);
			
			String roleExpression = 		(String)JbpmExpressionEvaluator.evaluate(getRoleExpressionExp(), ectx);
			Role role = JSONExpHandler.resolveRoleFromJSONExpression(roleExpression);
			
			ArrayList<Role> rolz = new ArrayList<Role>(1);
			rolz.add(role);
			
			getRolesManager().createProcessRoles(pi.getProcessDefinition().getName(), rolz, pi.getId());
			getRolesManager().createIdentitiesForRoles(rolz, userId.toString(), IdentityType.USER, pi.getId());
		}
	}
	
	public String getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(String processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public String getUserIdExp() {
		return userIdExp;
	}

	public void setUserIdExp(String userIdExp) {
		this.userIdExp = userIdExp;
	}

	public String getRoleExpressionExp() {
		return roleExpressionExp;
	}

	public void setRoleExpressionExp(String roleExpressionExp) {
		this.roleExpressionExp = roleExpressionExp;
	}
}