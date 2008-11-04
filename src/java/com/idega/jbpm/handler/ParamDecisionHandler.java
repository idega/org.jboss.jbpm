package com.idega.jbpm.handler;

import java.util.Map;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;

public interface ParamDecisionHandler extends DecisionHandler {

	public String decide(ExecutionContext ectx, Map<String, Object> parameterMap) throws Exception; 
}
