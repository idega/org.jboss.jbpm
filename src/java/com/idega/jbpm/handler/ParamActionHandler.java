package com.idega.jbpm.handler;

import java.util.Map;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public interface ParamActionHandler extends ActionHandler {

	public void execute(ExecutionContext ectx, Map<String, Object> parameterMap) throws Exception;
	
}
