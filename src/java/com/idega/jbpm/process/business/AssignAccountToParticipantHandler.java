package com.idega.jbpm.process.business;

import java.util.HashMap;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/03/25 10:35:57 $ by $Author: civilis $
 */
public class AssignAccountToParticipantHandler implements ActionHandler {

	private static final long serialVersionUID = -4163428065244816522L;
	public static final String participantUserIdVarName = "int:participantUserId";
	public static final String participantRoleNameVarName = "string:participantRoleName";
	
	public AssignAccountToParticipantHandler() { }
	
	public AssignAccountToParticipantHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {

		Integer userId = (Integer)ctx.getVariable(participantUserIdVarName);
		String roleName = (String)ctx.getVariable(participantRoleNameVarName);
		
		if(userId == null || roleName == null) {
			throw new IllegalArgumentException("Either is not provided - userId: "+userId+", roleName: "+roleName);
		}
		
		Role role = new Role();
		role.setRoleName(roleName);
		role.setScope(RoleScope.PI);
		
		HashMap<String, Role> rolz = new HashMap<String, Role>(1);
		rolz.put(role.getRoleName(), role);
		
		long parentProcessInstanceId = ctx.getProcessInstance().getSuperProcessToken().getProcessInstance().getId();
		
		getRolesManager().createRolesByProcessInstance(rolz, parentProcessInstanceId);
		getRolesManager().createIdentitiesForRoles(rolz.values(), String.valueOf(userId), IdentityType.USER, parentProcessInstanceId);
	}
	
	protected RolesManager getRolesManager() {
		return (RolesManager)WFUtil.getBeanInstance("bpmRolesManager");
	}
}