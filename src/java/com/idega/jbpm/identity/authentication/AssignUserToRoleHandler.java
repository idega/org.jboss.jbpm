package com.idega.jbpm.identity.authentication;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.Identity;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $ Last modified: $Date: 2009/02/23 12:36:38 $ by $Author: civilis $
 */
@Service("assignUserToRoleHandler")
@Scope("prototype")
public class AssignUserToRoleHandler implements ActionHandler {
	
	private static final long serialVersionUID = 2953390756074619221L;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	private Integer userId;
	private String roleExpression;
	private Long processInstanceId;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		if (getUserId() != null && getProcessInstanceId() != null
		        && getRoleExpression() != null) {
			
			Integer userId = getUserId();
			
			Long pid = getProcessInstanceId();
			ProcessInstance pi = ectx.getJbpmContext().getProcessInstance(pid);
			
			String roleExpression = getRoleExpression();
			
			Role role = JSONExpHandler
			        .resolveRoleFromJSONExpression(roleExpression);
			
			ArrayList<Role> rolz = new ArrayList<Role>(1);
			rolz.add(role);
			
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			
			rolesManager.createProcessActors(rolz, pi);
			rolesManager.createIdentitiesForRoles(rolz, new Identity(userId
			        .toString(), IdentityType.USER), pi.getId());
		} else {
			Logger
			        .getLogger(getClass().getName())
			        .log(
			            Level.WARNING,
			            "Called assign user to role handler, but inssufficient parameters provided. User id = "
			                    + getUserId()
			                    + ", process instance id = "
			                    + getProcessInstanceId()
			                    + ", role expression = "
			                    + getRoleExpression()
			                    + ", called from process instance id = "
			                    + ectx.getProcessInstance().getId());
		}
	}
	
	public Integer getUserId() {
		return userId;
	}
	
	public void setUserId(Integer userId) {
		this.userId = userId;
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