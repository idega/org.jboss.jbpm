package com.idega.jbpm.variables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


@Scope("singleton")
@Service("bpmVariableResolver")
public class VariablesResolver {

	private ThreadLocal<ExecutionContext> executionContextLocal = new ThreadLocal<ExecutionContext>();
	private static Log log = LogFactory.getLog(VariablesResolver.class);
	
	
	public void setExecutionContext(ExecutionContext ectx){
		executionContextLocal.set(ectx);
	}
	
	/**
	 * Returns a variable with that matching name
	 * 
	 * @param variableName
	 * @return
	 */
	public Object get(String variableName){
		ExecutionContext ectx = executionContextLocal.get();
		if(ectx != null){
			return ectx.getContextInstance().getVariable(variableName, ectx.getToken());
		}else{
			log.warn("Exection context was not set before resolving variable: " + variableName);
	
			//TODO: fix this??
			return null;
		}
		
	}
	
}
