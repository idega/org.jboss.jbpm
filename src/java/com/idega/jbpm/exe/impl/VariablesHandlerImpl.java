package com.idega.jbpm.exe.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/12 15:43:03 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class VariablesHandlerImpl implements VariablesHandler {

	private IdegaJbpmContext idegaJbpmContext;
	
	public void submitVariables(Map<String, Object> variables, long taskInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			if(variables == null || variables.isEmpty())
				return;

			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			TaskController tiController = ti.getTask().getTaskController();
			
			if(tiController == null)
//				no variables perhaps?
				return;
			
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = tiController.getVariableAccesses();
			
			for (VariableAccess variableAccess : variableAccesses)
				if(!variableAccess.isWritable() && variables.containsKey(variableAccess.getVariableName()))
					variables.remove(variableAccess.getVariableName());
			
//			TODO: remove variables, that don't exist in process definition

			ti.setVariables(variables);
			ti.getTask().getTaskController().submitParameters(ti);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Map<String, Object> populateVariables(long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			@SuppressWarnings("unchecked")
			Map<String, Object> variables = (Map<String, Object>)ti.getVariables();
			
			return variables;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Map<String, Object> populateVariablesFromProcess(long processInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessInstance pi = ctx.getProcessInstance(processInstanceId);
			
			@SuppressWarnings("unchecked")
			Collection<TaskInstance> taskInstances = pi.getTaskMgmtInstance().getTaskInstances();
			Map<String, Object> variables = new HashMap<String, Object>();
			
			for (TaskInstance taskInstance : taskInstances) {

				@SuppressWarnings("unchecked")
				Map<String, Object> taskInstanceVariables = (Map<String, Object>)taskInstance.getVariables();
				variables.putAll(taskInstanceVariables);
			}
			
			return variables;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
}