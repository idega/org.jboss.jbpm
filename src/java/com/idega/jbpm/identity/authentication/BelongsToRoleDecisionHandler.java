package com.idega.jbpm.identity.authentication;

import java.security.AccessControlException;
import java.security.Permission;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;

/**
 * Jbpm action handler, checks if <b>current</b> user belongs to process role
 * provided. Output is boolean string expression (true/false)
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 *          Last modified: $Date: 2008/11/30 08:17:52 $ by $Author: civilis $
 */
@Service("belongsToRoleDecisionHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BelongsToRoleDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = -5509068763021941599L;
	private String roleExpression;
	private Long processInstanceId;

	@Autowired
	private BPMFactory bpmFactory;

	private static final String booleanTrue = "true";
	private static final String booleanFalse = "false";

	@Override
	public String decide(ExecutionContext ectx) throws Exception {

		final String roleExpression = getRoleExpression();
		Role role = JSONExpHandler
				.resolveRoleFromJSONExpression(roleExpression, ectx);

		final Long processInstanceId = getProcessInstanceId();

		// create permission to check against, only process id and role name is
		// used, as only currently logged in user is checked
		Permission perm = getBpmFactory().getRolesManager()
				.getPermissionsFactory().getRoleAccessPermission(
						processInstanceId, role.getRoleName(), false);

		try {
			getBpmFactory().getRolesManager().checkPermission(perm);

		} catch (AccessControlException e) {

			return booleanFalse;
		}

		return booleanTrue;
	}

	public String getRoleExpression() {
		return roleExpression;
	}

	public void setRoleExpression(String roleExpression) {
		this.roleExpression = roleExpression;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}