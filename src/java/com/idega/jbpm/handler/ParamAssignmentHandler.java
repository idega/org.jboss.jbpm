package com.idega.jbpm.handler;

import java.util.Map;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

public interface ParamAssignmentHandler extends AssignmentHandler {

	public void assign(Assignable ass, ExecutionContext ectx, Map<String, Object> parameterMap) throws Exception;
	
}
