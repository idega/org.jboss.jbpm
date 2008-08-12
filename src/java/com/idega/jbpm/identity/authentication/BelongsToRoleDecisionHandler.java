package com.idega.jbpm.identity.authentication;

import java.security.AccessControlException;
import java.security.Permission;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.util.expression.ELUtil;

/**
 * Jbpm action handler, checks if <b>current</b> user belongs to process role provided. Output is boolean string expression (true/false)
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/08/12 10:58:30 $ by $Author: civilis $
 */
public class BelongsToRoleDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = -5509068763021941599L;
	private String roleExpressionExp;
	private String processInstanceIdExp;
	private RolesManager rolesManager;
	private static final String booleanTrue = 	"true";
	private static final String booleanFalse = 	"false";
	@Autowired
	private PermissionsFactory permissionsFactory;
	
	public String decide(ExecutionContext ectx) throws Exception {
		
//		injecting spring dependencies, as this is not yet spring managed bean
//		TODO: remove this when moved to spring (or seam) bean
		ELUtil.getInstance().autowire(this);
		
		final String roleExpression =		(String)JbpmExpressionEvaluator.evaluate(getRoleExpressionExp(), ectx);
		Role role = JSONExpHandler.resolveRoleFromJSONExpression(roleExpression);
		
		final Long processInstanceId =		(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
		
//		create permission to check against, only process id and role name is used, as only currently logged in user is checked
		Permission perm = getPermissionsFactory().getRoleAccessPermission(processInstanceId, role.getRoleName(), false);
		
		try {
			getRolesManager().checkPermission(perm);
			
		} catch (AccessControlException e) {

			return booleanFalse;
		}
		
		return booleanTrue;
	}

	public String getRoleExpressionExp() {
		return roleExpressionExp;
	}

	public void setRoleExpressionExp(String roleExpressionExp) {
		this.roleExpressionExp = roleExpressionExp;
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

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public void setPermissionsFactory(PermissionsFactory permissionsFactory) {
		this.permissionsFactory = permissionsFactory;
	}
}