package com.idega.jbpm.identity;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 *          Last modified: $Date: 2008/11/30 08:17:27 $ by $Author: civilis $
 */
@Service("createBPMUserHandler")
@Scope("prototype")
public class CreateBPMUserHandler implements ActionHandler {

	private static final long serialVersionUID = -8830098201665066649L;
	private Long processInstanceId;
	private UserPersonalData userData;
	private String roleExpression;
	private String bpmUserIdVariableName;
	@Autowired private BPMFactory bpmFactory;

	public void execute(ExecutionContext ectx) throws Exception {

		Long pid = getProcessInstanceId();
		UserPersonalData upd = getUserData();
		String roleExpression = getRoleExpression();

		Role role = JSONExpHandler
				.resolveRoleFromJSONExpression(roleExpression);

		User bpmUser = getBpmFactory().getBpmUserFactory().createBPMUser(upd,
				role, pid);

		if (getBpmUserIdVariableName() != null) {

			String variableName = getBpmUserIdVariableName();

			final Object pk = bpmUser.getPrimaryKey();
			final Integer usrId;

			if (pk instanceof Integer)
				usrId = (Integer) pk;
			else
				usrId = new Integer(pk.toString());

			ectx.setVariable(variableName, usrId);
		}
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public UserPersonalData getUserData() {
		return userData;
	}

	public void setUserData(UserPersonalData userData) {
		this.userData = userData;
	}

	public String getRoleExpression() {
		return roleExpression;
	}

	public void setRoleExpression(String roleExpression) {
		this.roleExpression = roleExpression;
	}

	public String getBpmUserIdVariableName() {
		return bpmUserIdVariableName;
	}

	public void setBpmUserIdVariableName(String bpmUserIdVariableName) {
		this.bpmUserIdVariableName = bpmUserIdVariableName;
	}
}