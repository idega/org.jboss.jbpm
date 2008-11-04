package com.idega.jbpm.handler;

import java.util.Map;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.TaskInstance;

public interface ParamTaskControllerHandler extends TaskControllerHandler {

	public void submitTaskVariables(TaskInstance arg0, ContextInstance arg1,
			Token arg2, Map<String, Object> parameterMap);
	
	public void initializeTaskVariables(TaskInstance arg0, ContextInstance arg1, 
			Token arg2, Map<String, Object> parameterMap);
	
}
