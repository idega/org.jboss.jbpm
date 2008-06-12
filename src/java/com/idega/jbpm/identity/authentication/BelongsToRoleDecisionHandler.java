package com.idega.jbpm.identity.authentication;

import java.security.AccessControlException;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.RoleAccessPermission;
import com.idega.util.expression.ELUtil;

/**
 * Jbpm action handler, checks if <b>current</b> user belongs to process role provided. Output is boolean string expression (true/false)
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/06/12 18:29:53 $ by $Author: civilis $
 */
public class BelongsToRoleDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = -5509068763021941599L;
	private String roleNameExp;
	private String processInstanceIdExp;
	private RolesManager rolesManager;
	private static final String booleanTrue = 	"true";
	private static final String booleanFalse = 	"false";
	
	public String decide(ExecutionContext ectx) throws Exception {
		
//		injecting spring dependencies, as this is not yet spring managed bean
//		TODO: remove this when moved to spring (or seam) bean
		ELUtil.getInstance().autowire(this);
		
		final String roleName =			(String)JbpmExpressionEvaluator.evaluate(getRoleNameExp(), ectx);
		final Long processInstanceId =	(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
		
//		create permission to check against, only process id and role name is used, as only currently logged in user is checked
		final RoleAccessPermission perm = new RoleAccessPermission("roleAccess", null);
		perm.setProcessInstanceId(processInstanceId);
		perm.setRoleName(roleName);
		
		try {
			getRolesManager().checkPermission(perm);
			
		} catch (AccessControlException e) {

			return booleanFalse;
		}
		
		return booleanTrue;
	}

	public String getRoleNameExp() {
		return roleNameExp;
	}

	public void setRoleNameExp(String roleNameExp) {
		this.roleNameExp = roleNameExp;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public String getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(String processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}
}