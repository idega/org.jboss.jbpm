package com.idega.jbpm.task;

import java.util.List;

import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/02 15:35:04 $ by $Author: civilis $
 */
@Service("taskControllerDefault")
@Scope("prototype")
public class TaskControllerDefaultHandler implements TaskControllerHandler {
	
	private static final long serialVersionUID = 9022017251041205372L;
	
	public void initializeTaskVariables(TaskInstance taskInstance,
	        ContextInstance contextInstance, Token token) {
		
		TaskController taskController = taskInstance.getTask()
		        .getTaskController();
		
		if (taskController != null) {
			
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = taskController
			        .getVariableAccesses();
			
			if (variableAccesses != null) {
				
				for (VariableAccess variableAccess : variableAccesses) {
					
					String mappedName = variableAccess.getMappedName();
					if (variableAccess.isReadable()) {
						String variableName = variableAccess.getVariableName();
						Object value = contextInstance.getVariable(
						    variableName, token);
						taskInstance.setVariableLocally(mappedName, value);
					} else {
						taskInstance.setVariableLocally(mappedName, null);
					}
				}
			}
		}
	}
	
	public void submitTaskVariables(TaskInstance taskInstance,
	        ContextInstance contextInstance, Token token) {
		
		TaskController taskController = taskInstance.getTask()
		        .getTaskController();
		
		if (taskController != null) {
			
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = taskController
			        .getVariableAccesses();
			
			if (variableAccesses != null) {
				String missingTaskVariables = null;
				
				for (VariableAccess variableAccess : variableAccesses) {
					
					String mappedName = variableAccess.getMappedName();
					// first check if the required variableInstances are present
					if ((variableAccess.isRequired())
					        && (!taskInstance.hasVariableLocally(mappedName))) {
						if (missingTaskVariables == null) {
							missingTaskVariables = mappedName;
						} else {
							missingTaskVariables += ", " + mappedName;
						}
					}
				}
				
				// if there are missing, required parameters, throw an
				// IllegalArgumentException
				if (missingTaskVariables != null) {
					throw new IllegalArgumentException(
					        "missing task variables: " + missingTaskVariables);
				}
				
				for (VariableAccess variableAccess : variableAccesses) {
					
					String mappedName = variableAccess.getMappedName();
					String variableName = variableAccess.getVariableName();
					
					if (variableAccess.isWritable()) {
						
						Object value = taskInstance.getVariable(mappedName);
						
						if (value != null) {
							
							// we set it first for relatively global scope, or how jbpm it sets by
							// default (looks for first token variable map, and usually this ends up
							// to root token variable map)
							contextInstance.setVariable(variableName, value,
							    token);
							
							// then we set it directly on token scope, so we have a really scoped
							// variable. jbpm doesn't do that on it's own
							contextInstance.setVariableLocally(variableName,
							    value, token);
						}
					}
				}
			}
		}
	}
}