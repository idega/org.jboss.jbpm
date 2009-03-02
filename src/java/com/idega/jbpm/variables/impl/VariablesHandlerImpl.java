package com.idega.jbpm.variables.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.jbpm.variables.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.16 $ Last modified: $Date: 2009/03/02 15:36:19 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmVariablesHandler")
@Transactional(readOnly = true)
public class VariablesHandlerImpl implements VariablesHandler {
	
	private BPMContext idegaJbpmContext;
	private BinaryVariablesHandler binaryVariablesHandler;
	
	@Transactional(readOnly = false)
	public void submitVariables(final Map<String, Object> variables,
	        final long taskInstanceId, final boolean validate) {
		
		if (variables == null || variables.isEmpty())
			return;
		
		getIdegaJbpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				
				if (ti.hasEnded())
					throw new TaskInstanceVariablesException(
					        "Task instance has already ended");
				
				TaskController tiController = ti.getTask().getTaskController();
				
				if (tiController == null)
					// this occurs when no variables defined in the controller
					return null;
				
				if (validate) {
					
					@SuppressWarnings("unchecked")
					List<VariableAccess> variableAccesses = tiController
					        .getVariableAccesses();
					
					for (VariableAccess variableAccess : variableAccesses) {
						
						if (variableAccess.getAccess().isRequired()
						        && !variables.containsKey(variableAccess
						                .getVariableName()))
							throw new TaskInstanceVariablesException(
							        "Required variable ("
							                + variableAccess.getVariableName()
							                + ") not submitted.");
						
						if (!variableAccess.isWritable()
						        && variables.containsKey(variableAccess
						                .getVariableName())) {
							
							Logger.getLogger(getClass().getName()).log(
							    Level.WARNING,
							    "Tried to submit read-only variable ("
							            + variableAccess.getVariableName()
							            + "), ignoring.");
							variables.remove(variableAccess.getVariableName());
						}
					}
				}
				
				final Map<String, Object> variablesToSubmit = getBinaryVariablesHandler()
				        .storeBinaryVariables(taskInstanceId, variables);
				
				ti.setVariables(variablesToSubmit);
				
				return null;
			}
		});
	}
	
	@Transactional(readOnly = false)
	public Map<String, Object> submitVariablesExplicitly(
	        final Map<String, Object> originalVariables,
	        final long taskInstanceId) {
		
		if (originalVariables == null || originalVariables.isEmpty())
			return null;
		
		return getIdegaJbpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				
				final Map<String, Object> variables = getBinaryVariablesHandler()
				        .storeBinaryVariables(taskInstanceId, originalVariables);
				
				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				Token taskInstanceToken = ti.getToken();
				
				@SuppressWarnings("unchecked")
				Map<String, VariableInstance> varInstances = ti
				        .getVariableInstances();
				
				for (Entry<String, Object> entry : variables.entrySet()) {
					
					if (varInstances != null
					        && !varInstances.containsKey(entry.getKey())) {
						
						VariableInstance vi = VariableInstance
						        .create(taskInstanceToken, entry.getKey(),
						            entry.getValue());
						ti.addVariableInstance(vi);
					}
				}
				
				@SuppressWarnings("unchecked")
				Map<String, Object> vars = ti.getVariables();
				vars.putAll(variables);
				ti.setVariables(vars);
				
				return variables;
			}
		});
	}
	
	public Map<String, Object> populateVariables(final long taskInstanceId) {
		
		return getIdegaJbpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				
				Map<String, Object> variables = new HashMap<String, Object>(ti
				        .getVariablesLocally());
				
				if (!ti.hasEnded()) {
					
					// Map<String, VariableInstance> variablesInstances;
					//				
					// variablesInstances = ti
					// .getVariableInstances();
					//					
					// variables = new HashMap<String, Object>(variablesInstances
					// .size());
					
					@SuppressWarnings("unchecked")
					List<VariableAccess> accesses = ti.getTask()
					        .getTaskController().getVariableAccesses();
					
					// String tokenReadAccess = "mamamia";
					
					for (VariableAccess variableAccess : accesses) {
						
						if (!variables.containsKey(variableAccess
						        .getVariableName())) {
							
							// the situation when process definition was changed (using formbuilder
							// for instance)
							// but task instance was created already
							
							if (variableAccess.isReadable()) {
								
								// read - populating from token
								// TODO: test if this populates variable from the token
								Object variable = ti.getContextInstance()
								        .getVariable(
								            variableAccess.getVariableName());
								variables.put(variableAccess.getVariableName(),
								    variable);
							}
						}
						
						if (variableAccess.isWritable()
						        && !variableAccess.isReadable()) {
							
							// we don't want to show non readable variable
							// this is backward compatibility, for task instances, that were created
							// with wrong process definition
							
							variables.remove(variableAccess.getVariableName());
						}
						
						/*
						if (!variableAccess.isWritable() || variableAccess.getAccess().hasAccess(tokenReadAccess)) {
							
							VariableInstance variableInstance = variablesInstances
						        .get(variableAccess.getVariableName());
						
							variables.put(variableAccess.getVariableName(),
							    variableInstance.getValue());
						}
						*/
					}
				}
				// else {
				// variables = new HashMap<String, Object>(ti.getVariablesLocally());
				// }
				
				// if (variablesInstances != null) {
				// variables = new HashMap<String, Object>(variablesInstances
				// .size());
				// } else {
				// variables = Collections.emptyMap();
				// }
				
				// readonly
				/*
				if (ti.hasEnded()) {
					
				//					for (VariableInstance variableInstance : variablesInstances
				//					        .values()) {
				//						variables.put(variableInstance.getName(),
				//						    variableInstance.getValue());
				//					}
					
				} else {
					
					@SuppressWarnings("unchecked")
					List<VariableAccess> accesses = ti.getTask()
					        .getTaskController().getVariableAccesses();
					
					String tokenReadAccess = "mamamia";
					
					for (VariableAccess variableAccess : accesses) {
						
						if (!variableAccess.isWritable() || variableAccess.getAccess().hasAccess(tokenReadAccess)) {
							
							VariableInstance variableInstance = variablesInstances
						        .get(variableAccess.getVariableName());
						
							variables.put(variableAccess.getVariableName(),
							    variableInstance.getValue());
						}
					}
				}
				*/

				return variables;
			}
		});
	}
	
	public List<BinaryVariable> resolveBinaryVariables(long taskInstanceId) {
		
		Map<String, Object> variables = populateVariables(taskInstanceId);
		return getBinaryVariablesHandler().resolveBinaryVariablesAsList(
		    variables);
	}
	
	public List<BinaryVariable> resolveBinaryVariables(long taskInstanceId,
	        Variable variable) {
		
		// tmp solution
		Map<String, Object> variables = populateVariables(taskInstanceId);
		List<BinaryVariable> binVars = getBinaryVariablesHandler()
		        .resolveBinaryVariablesAsList(variables);
		
		if (binVars != null) {
			
			for (Iterator<BinaryVariable> iterator = binVars.iterator(); iterator
			        .hasNext();) {
				BinaryVariable binaryVariable = iterator.next();
				
				if (!variable.getDefaultStringRepresentation().equals(
				    binaryVariable.getVariable()
				            .getDefaultStringRepresentation())) {
					iterator.remove();
				}
			}
		}
		
		return binVars;
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