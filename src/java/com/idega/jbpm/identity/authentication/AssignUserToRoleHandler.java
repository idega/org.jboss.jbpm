package com.idega.jbpm.identity.authentication;

import java.util.ArrayList;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.util.expression.ELUtil;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2008/11/13 15:08:32 $ by $Author: juozas $
 */
@Service("assignUserToRoleHandler")
@Scope("prototype")
public class AssignUserToRoleHandler implements ActionHandler {

	private static final long serialVersionUID = 2953390756074619221L;
	private RolesManager rolesManager;
	private Integer userIdExp;
	private String roleExpressionExp;
	private Long processInstanceIdExp;
	
	public void execute(ExecutionContext ectx) throws Exception {

		if(getUserIdExp() != null && getProcessInstanceIdExp() != null && getRoleExpressionExp() != null) {

			ELUtil.getInstance().autowire(this);
			
			Integer userId = getUserIdExp();//	(Integer)JbpmExpressionEvaluator.evaluate(getUserIdExp(), ectx);
			
			Long pid = 	getProcessInstanceIdExp();		//(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
			ProcessInstance pi = ectx.getJbpmContext().getProcessInstance(pid);
			
			String roleExpression = getRoleExpressionExp();		//(String)JbpmExpressionEvaluator.evaluate(getRoleExpressionExp(), ectx);
			Role role = JSONExpHandler.resolveRoleFromJSONExpression(roleExpression);
			
			ArrayList<Role> rolz = new ArrayList<Role>(1);
			rolz.add(role);
			
			getRolesManager().createProcessRoles(pi.getProcessDefinition().getName(), rolz, pi.getId());
			getRolesManager().createIdentitiesForRoles(rolz, userId.toString(), IdentityType.USER, pi.getId());
		}
	}
	
	public Long getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(Long processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public Integer getUserIdExp() {
		return userIdExp;
	}

	public void setUserIdExp(Integer userIdExp) {
		this.userIdExp = userIdExp;
	}

	public String getRoleExpressionExp() {
		return roleExpressionExp;
	}

	public void setRoleExpressionExp(String roleExpressionExp) {
		this.roleExpressionExp = roleExpressionExp;
	}
}