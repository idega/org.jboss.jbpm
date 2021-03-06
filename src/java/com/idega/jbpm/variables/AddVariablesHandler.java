package com.idega.jbpm.variables;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.TaskInstanceW;

@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("addVariablesHandler")
public class AddVariablesHandler implements ActionHandler {
	
	private static final long serialVersionUID = 2507035610551135901L;
	private long taskInstanceId;
	private String variableName;
	private Object value;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private VariablesHandler variablesHandler;
	
	public void execute(ExecutionContext executionContext) throws Exception {
		
		Variable variable = Variable
		        .parseDefaultStringRepresentation(getVariableName());
		
		TaskInstanceW tiw = getBpmFactory().getTaskInstanceW(
		    getTaskInstanceId());
		tiw.addVariable(variable, getValue());
	}
	
	public long getTaskInstanceId() {
		return taskInstanceId;
	}
	
	public String getVariableName() {
		return variableName;
	}
	
	public Object getValue() {
		return value;
	}
	
	public void setTaskInstanceId(long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public VariablesHandler getVariablesHandler() {
		return variablesHandler;
	}
	
	public void setVariablesHandler(VariablesHandler variablesHandler) {
		this.variablesHandler = variablesHandler;
	}
	
}
