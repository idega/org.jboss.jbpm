package com.idega.jbpm.proxy;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import bsh.Interpreter;

import com.idega.jbpm.variables.VariablesResolver;

/**
 * Helper class to evaluate scripts in jpdl
 * 
 * @author juozas
 */
@Service
@Scope("singleton")
public class ScriptEvaluator {
	
	@Autowired
	private VariablesResolver variablesResolver;
	
	/**
	 * Evaluates a script
	 * 
	 * @param script
	 *            that should be evaluated
	 * @param executionContext
	 *            - from witch variables should be resolved
	 * @return the result of the script
	 * @throws Exception
	 *             is usually thrown when script has errors
	 */
	public Object evaluate(String script, Map<String, Object> inputMap)
	        throws Exception {
		
		try {
			Interpreter interpreter = new Interpreter();
			for (String inputName : inputMap.keySet()) {
				Object inputValue = inputMap.get(inputName);
				interpreter.set(inputName, inputValue);
			}
			return interpreter.eval(script);
			
		} catch (Exception e) {
			// try to throw the cause of the EvalError
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			} else if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else {
				throw e;
			}
		}
	}
	
	public Object evaluate(String script, ExecutionContext executionContext)
	        throws Exception {
		
		Map<String, Object> inputMap = createInputMap(executionContext);
		return evaluate(script, inputMap);
	}
	
	public Map<String, Object> createInputMap(ExecutionContext executionContext) {
		Token token = executionContext.getToken();
		VariablesResolver resolver = getVariablesResolver();
		resolver.setExecutionContext(executionContext);
		
		Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put("executionContext", executionContext);
		inputMap.put("token", token);
		inputMap.put("node", executionContext.getNode());
		inputMap.put("task", executionContext.getTask());
		inputMap.put("taskInstance", executionContext.getTaskInstance());
		inputMap.put("resolver", resolver);
		
		return inputMap;
	}
	
	VariablesResolver getVariablesResolver() {
		return variablesResolver;
	}
}