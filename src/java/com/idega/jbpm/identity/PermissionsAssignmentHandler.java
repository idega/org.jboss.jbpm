package com.idega.jbpm.identity;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;

@Service("premissionsAssignmentHandler")
@Scope("prototype")
public class PermissionsAssignmentHandler implements ActionHandler {
	
	private static final long serialVersionUID = 3245947556356612146L;
	private Long taskInstanceId;
	private String expression;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private JSONAssignmentHandler jsonAssignmentHandler;
	
	public void execute(ExecutionContext executionContext) throws Exception {
		
		getJsonAssignmentHandler().setExpression(expression);
		TaskInstance taskInstance = executionContext.getJbpmContext()
		        .getTaskInstance(taskInstanceId);
		getJsonAssignmentHandler().assign(taskInstance, executionContext);
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
	
	public JSONAssignmentHandler getJsonAssignmentHandler() {
		return jsonAssignmentHandler;
	}
	
	public void setJsonAssignmentHandler(
	        JSONAssignmentHandler jsonAssignmentHandler) {
		this.jsonAssignmentHandler = jsonAssignmentHandler;
	}
	
}
