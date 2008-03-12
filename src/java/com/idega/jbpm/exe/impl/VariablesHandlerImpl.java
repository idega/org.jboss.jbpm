package com.idega.jbpm.exe.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/12 20:43:43 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class VariablesHandlerImpl implements VariablesHandler {

	private IdegaJbpmContext idegaJbpmContext;
	
	public void submitVariables(Map<String, Object> variables, long taskInstanceId, boolean validate) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			if(variables == null || variables.isEmpty())
				return;

			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			TaskController tiController = ti.getTask().getTaskController();
			
			if(tiController == null)
//				this occurs when no variables defined in the controller
				return;
			
			if(validate) {
			
				@SuppressWarnings("unchecked")
				List<VariableAccess> variableAccesses = tiController.getVariableAccesses();
				
				for (VariableAccess variableAccess : variableAccesses) {
					
					if(variableAccess.getAccess().isRequired() && !variables.containsKey(variableAccess.getVariableName()))
						throw new RuntimeException("Required variable ("+variableAccess.getVariableName()+") not submitted.");
					
					if(!variableAccess.isWritable() && variables.containsKey(variableAccess.getVariableName())) {
						
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "Tried to submit read-only variable ("+variableAccess.getVariableName()+"), ignoring.");
						variables.remove(variableAccess.getVariableName());
					}
				}
			}
			
			ti.setVariables(variables);
			tiController.submitParameters(ti);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Map<String, Object> populateVariables(long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			@SuppressWarnings("unchecked")
			Map<String, Object> localVariables = ti.getVariablesLocally();
			
			@SuppressWarnings("unchecked")
			Map<String, Object> globalVariables = ti.getVariables();
			
			HashMap<String, Object> variables = new HashMap<String, Object>(globalVariables);
			
//			readonly
			if(ti.hasEnded()) {
				
//				override with local ones
				variables.putAll(localVariables);
				
				return variables;
				
			} else {
				
				for (Entry<String, Object> entry : localVariables.entrySet()) {
				
//					override with existing local values
					if(entry.getValue() != null)
						variables.put(entry.getKey(), entry.getValue());
				}
				
				return variables;
			}
			
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