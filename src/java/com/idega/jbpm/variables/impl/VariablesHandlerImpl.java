package com.idega.jbpm.variables.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.jbpm.variables.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/10/01 09:31:48 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmVariablesHandler")
public class VariablesHandlerImpl implements VariablesHandler {

	private BPMContext idegaJbpmContext;
	private BinaryVariablesHandler binaryVariablesHandler;
	
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
			
			variables = getBinaryVariablesHandler().storeBinaryVariables(String.valueOf(taskInstanceId), variables);
			
			ti.setVariables(variables);
			///tiController.submitParameters(ti);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public Map<String, Object> submitVariablesExplicitly(Map<String, Object> variables, long taskInstanceId) {
		
		if(variables == null || variables.isEmpty())
			return null;
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			variables = getBinaryVariablesHandler().storeBinaryVariables(String.valueOf(taskInstanceId), variables);

			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			Token taskInstanceToken = ti.getToken();
			
			@SuppressWarnings("unchecked")
			Map<String, VariableInstance> varInstances = ti.getVariableInstances();
			
			for (Entry<String, Object> entry : variables.entrySet()) {
				
				if(!varInstances.containsKey(entry.getKey())) {
				
					VariableInstance vi = VariableInstance.create(taskInstanceToken, entry.getKey(), entry.getValue());
					ti.addVariableInstance(vi);
				}
			}
			
			return variables;
			
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
	
	public List<BinaryVariable> resolveBinaryVariables(long taskInstanceId) {
	
		Map<String, Object> variables = populateVariables(taskInstanceId);
		return getBinaryVariablesHandler().resolveBinaryVariablesAsList(variables);
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
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

	public BinaryVariablesHandler getBinaryVariablesHandler() {
		return binaryVariablesHandler;
	}

	@Autowired
	public void setBinaryVariablesHandler(
			BinaryVariablesHandler binaryVariablesHandler) {
		this.binaryVariablesHandler = binaryVariablesHandler;
	}
}