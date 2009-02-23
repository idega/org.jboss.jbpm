package com.idega.jbpm.process.business;

import java.util.ArrayList;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.identity.Identity;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $ Last modified: $Date: 2009/02/23 12:36:38 $ by $Author: civilis $
 */
public class AssignAccountToParticipantHandler implements ActionHandler {
	
	private static final long serialVersionUID = -4163428065244816522L;
	public static final String participantUserIdVarName = "int_participantUserId";
	public static final String participantRoleNameVarName = "string_participantRoleName";
	
	public AssignAccountToParticipantHandler() {
	}
	
	public AssignAccountToParticipantHandler(String parm) {
	}
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		Integer userId = (Integer) ectx.getVariable(participantUserIdVarName);
		String roleName = (String) ectx.getVariable(participantRoleNameVarName);
		
		if (userId == null || roleName == null) {
			throw new IllegalArgumentException(
			        "Either is not provided - userId: " + userId
			                + ", roleName: " + roleName);
		}
		
		Role role = new Role();
		role.setRoleName(roleName);
		role.setScope(RoleScope.PI);
		
		ArrayList<Role> rolz = new ArrayList<Role>(1);
		rolz.add(role);
		
		ProcessInstance parentPI = ectx.getProcessInstance()
		        .getSuperProcessToken().getProcessInstance();
		
		getRolesManager().createProcessActors(rolz, parentPI);
		getRolesManager().createIdentitiesForRoles(rolz,
		    new Identity(String.valueOf(userId), IdentityType.USER),
		    parentPI.getId());
	}
	
	protected RolesManager getRolesManager() {
		return (RolesManager) WFUtil.getBeanInstance("bpmRolesManager");
	}
}