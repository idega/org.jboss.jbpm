package com.idega.jbpm.identity;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;

@Service("premissionsAssignmentHandler")
@Scope("prototype")
public class PermissionsAssignmentHandler implements ActionHandler {

	private Long taskInstanceId;
	private String expression;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	public void execute(ExecutionContext executionContext) throws Exception {
		TaskAssignment ta = JSONExpHandler.resolveRolesFromJSONExpression(expression);
		
		TaskInstance taskInstance = executionContext.getJbpmContext().getTaskInstance(taskInstanceId);
		if(taskInstance == null){
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
				"No task with id: " + taskInstanceId);
			return;
		}
		
		List<Role> roles = ta.getRoles();

		if (roles == null || roles.isEmpty()) {

			Logger.getLogger(getClass().getName()).log(Level.WARNING,
					"No roles for task instance: " + taskInstanceId);
			return;
		}
		
		
		
		ProcessInstance pi;

		if (ta.getRolesFromProcessInstanceId() != null) {

			pi = executionContext.getJbpmContext().getProcessInstance(
					ta.getRolesFromProcessInstanceId());
		} else {
			pi = executionContext.getProcessInstance();
		}
		
		TaskInstanceW tiw = getBpmFactory().getProcessManagerByProcessInstanceId(pi.getId())
		   .getTaskInstance(taskInstanceId);

		for(Role role:roles){
			tiw.setTaskRolePermissions(role,true,null);
		}
	}
	
	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getExpression() {
		return expression;
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}

}
