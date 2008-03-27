package com.idega.jbpm.exe.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/03/27 14:14:03 $ by $Author: civilis $
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
			
			if(ti.hasEnded())
				throw new TaskInstanceVariablesException("Task instance has already ended");
			
			TaskController tiController = ti.getTask().getTaskController();
			
			if(tiController == null)
//				this occurs when no variables defined in the controller
				return;
			
			if(validate) {
			
				@SuppressWarnings("unchecked")
				List<VariableAccess> variableAccesses = tiController.getVariableAccesses();
				
				for (VariableAccess variableAccess : variableAccesses) {
					
					if(variableAccess.getAccess().isRequired() && !variables.containsKey(variableAccess.getVariableName()))
						throw new TaskInstanceVariablesException("Required variable ("+variableAccess.getVariableName()+") not submitted.");
					
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
			Map<String, VariableInstance> variablesInstances = ti.getVariableInstances();
			HashMap<String, Object> variables = new HashMap<String, Object>(variablesInstances.size());
			
//			readonly
			if(ti.hasEnded()) {
				
				for (VariableInstance variableInstance : variablesInstances.values()) {
					variables.put(variableInstance.getName(), variableInstance.getValue());
				}
				
			} else {
				
				@SuppressWarnings("unchecked")
				List<VariableAccess> accesses = ti.getTask().getTaskController().getVariableAccesses();
				
				for (VariableAccess access : accesses) {
					
					VariableInstance variableInstance = variablesInstances.get(access.getVariableName());
					
					if(!access.isWritable()) {
						variables.put(access.getVariableName(), variableInstance.getValue());
					}
				}
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
	
	public class TaskInstanceVariablesException extends RuntimeException {

		private static final long serialVersionUID = -8063364513129734369L;
		
		public TaskInstanceVariablesException() {
			super();
	    }

	    public TaskInstanceVariablesException(String s) {
	        super(s);
	    }

	    public TaskInstanceVariablesException(String s, Throwable throwable) {
	        super(s, throwable);
	    }

	    public TaskInstanceVariablesException(Throwable throwable) {
	        super(throwable);
	    }
	}
}