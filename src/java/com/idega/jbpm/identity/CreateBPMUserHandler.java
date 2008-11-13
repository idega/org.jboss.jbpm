package com.idega.jbpm.identity;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/11/13 15:08:32 $ by $Author: juozas $
 */
@Service("createBPMUserHandler")
@Scope("prototype")
public class CreateBPMUserHandler implements ActionHandler {

	private static final long serialVersionUID = -8830098201665066649L;
	private Long processInstanceIdExp;
	private UserPersonalData userDataExp;
	private String roleExpressionExp;
	private String bpmUserIdVariableNameExp;
	private BPMFactory bpmFactory;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		ELUtil.getInstance().autowire(this);
		
		Long pid = 			getProcessInstanceIdExp();//(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
		UserPersonalData upd = 	getUserDataExp();//	(UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
		String roleExpression = 	getRoleExpressionExp();//(String)JbpmExpressionEvaluator.evaluate(getRoleExpressionExp(), ectx);
		
		Role role = JSONExpHandler.resolveRoleFromJSONExpression(roleExpression);
		
		User bpmUser = getBpmFactory().getBpmUserFactory().createBPMUser(upd, role, pid);
		
		
		if(getBpmUserIdVariableNameExp() != null) {
		
			String variableName = getBpmUserIdVariableNameExp();//(String)JbpmExpressionEvaluator.evaluate(getBpmUserIdVariableNameExp(), ectx);
			
			final Object pk = bpmUser.getPrimaryKey();
			final Integer usrId;
			
			if(pk instanceof Integer)
				usrId = (Integer)pk;
			else
				usrId = new Integer(pk.toString());
			
			ectx.setVariable(variableName, usrId);
		}
	}

	public Long getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(Long processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}

	public UserPersonalData getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(UserPersonalData userDataExp) {
		this.userDataExp = userDataExp;
	}

	public String getRoleExpressionExp() {
		return roleExpressionExp;
	}

	public void setRoleExpressionExp(String roleExpressionExp) {
		this.roleExpressionExp = roleExpressionExp;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public String getBpmUserIdVariableNameExp() {
		return bpmUserIdVariableNameExp;
	}

	public void setBpmUserIdVariableNameExp(String bpmUserIdVariableNameExp) {
		this.bpmUserIdVariableNameExp = bpmUserIdVariableNameExp;
	}
}