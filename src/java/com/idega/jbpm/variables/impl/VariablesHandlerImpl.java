package com.idega.jbpm.variables.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskController;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.process.variables.Variable;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.event.VariableCreatedEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.variables.VariablesManager;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.DBUtil;
import com.idega.util.ListUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.20 $ Last modified: $Date: 2009/05/19 13:19:04 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("bpmVariablesHandler")
@Transactional(readOnly = true)
public class VariablesHandlerImpl implements VariablesHandler {

	private static final Logger LOGGER = Logger.getLogger(VariablesHandlerImpl.class.getName());

	private BPMContext idegaJbpmContext;
	private BinaryVariablesHandler binaryVariablesHandler;

	@Autowired
	private BPMFactory bpmFactory;

	@Override
	@Transactional(readOnly = false)
	public void submitVariables(final Map<String, Object> variables, final long taskInstanceId, final boolean validate) {
		if (MapUtil.isEmpty(variables))
			return;

		getIdegaJbpmContext().execute(new JbpmCallback<Void>() {
			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {
				submitVariables(context, variables, taskInstanceId, validate);
				return null;
			}
		});
	}

	private void doManageVariables(Long piId, Long tiId, Map<String, Object> variables) {
		try {
			IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
			Map<?, ?> beans = WebApplicationContextUtils.getWebApplicationContext(iwma.getServletContext()).getBeansOfType(VariablesManager.class);
			if (MapUtil.isEmpty(beans))
				return;

			for (Object bean: beans.values())
				((VariablesManager) bean).doManageVariables(piId, tiId, variables);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error managing variables (" + variables + ") for process: " + piId + " and task: " + tiId, e);
		}
	}

	@Override
	@Transactional(readOnly = false)
	public Map<String, Object> submitVariablesExplicitly(final Map<String, Object> originalVariables, final long taskInstanceId) {
		if (MapUtil.isEmpty(originalVariables))
			return null;

		return getIdegaJbpmContext().execute(new JbpmCallback<Map<String, Object>>() {

			@Override
			public Map<String, Object> doInJbpm(JbpmContext context) throws JbpmException {
				final Map<String, Object> variables = getBinaryVariablesHandler().storeBinaryVariables(taskInstanceId, originalVariables);

				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				Token taskInstanceToken = ti.getToken();

				@SuppressWarnings("unchecked")
				Map<String, VariableInstance> varInstances = ti.getVariableInstances();

				for (Entry<String, Object> entry: variables.entrySet()) {
					if (varInstances != null && !varInstances.containsKey(entry.getKey())) {
						try {
							VariableInstance vi = VariableInstance.create(taskInstanceToken, entry.getKey(), entry.getValue());
							ti.addVariableInstance(vi);
						} catch (Exception e) {
							String message = "Error creating variable " + entry.getKey() + " with value " + entry.getValue() + " for token " +
									taskInstanceToken + " and task instance: " + taskInstanceId;
							LOGGER.log(Level.WARNING, message, e);
							CoreUtil.sendExceptionNotification(message, e);
							throw new RuntimeException(message, e);
						}
					}
				}

				@SuppressWarnings("unchecked")
				Map<String, Object> vars = ti.getVariables();
				vars.putAll(variables);
				setVariables(ti, vars);

				ProcessInstance pi = ti.getProcessInstance();
				if (pi != null) {
					ELUtil.getInstance().publishEvent(new VariableCreatedEvent(this, pi.getProcessDefinition().getName(), pi.getId(), ti.getId(), vars));
				}

				return variables;
			}
		});
	}

	@Transactional(readOnly = false)
	private void setVariables(TaskInstance ti, Map<String, Object> variables) {
		ti.setVariables(variables);
	}

	@Override
	public Map<String, Object> populateVariables(final long taskInstanceId) {
		return getIdegaJbpmContext().execute(new JbpmCallback<Map<String, Object>>() {

			@SuppressWarnings("unchecked")
			@Override
			public Map<String, Object> doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance ti = context.getTaskInstance(taskInstanceId);

				Map<String, Object> vars = null;
				boolean loadByToken = false;
				try {
					vars = ti.getVariables();
				} catch (Exception e) {
					loadByToken = true;
					LOGGER.log(Level.WARNING, "Error loading variables for task: " + ti + ", ID: " + taskInstanceId, e);
				}
				if (MapUtil.isEmpty(vars) || loadByToken) {
					Token token = null;
					try {
						token = ti.getToken();
						if (token == null) {
							LOGGER.warning("Token is null for task instance: " + taskInstanceId);
						} else {
							token = DBUtil.getInstance().initializeAndUnproxy(token);
							vars = bpmFactory.getTaskInstanceW(taskInstanceId).getVariables(token);
						}
					} catch (Throwable e) {
						LOGGER.log(Level.WARNING, "Error loading variables for task: " + ti + ", ID: " + taskInstanceId + " by token: " + token +
								(token == null ? CoreConstants.EMPTY : token.getId()), e);
					}
				}
				Map<String, Object> variables = vars == null ? new HashMap<String, Object>() : new HashMap<String, Object>(vars);

				if (!ti.hasEnded()) {
					List<VariableAccess> accesses = ti.getTask().getTaskController().getVariableAccesses();

					for (VariableAccess variableAccess: accesses) {
						String varName = variableAccess.getVariableName();

						if (!variables.containsKey(varName)) {
							// the situation when process definition was changed (using formbuilder for instance) but task instance was created already
							if (variableAccess.isReadable()) {
								// read - populating from token
								ContextInstance ci = ti.getContextInstance();
								if (ci == null) {
									ProcessInstance pi = ti.getProcessInstance();
									if (pi != null)
										ci = pi.getContextInstance();
								}
								if (ci == null) {
									LOGGER.warning("Unable to get " + ContextInstance.class.getName() + " while trying to load variable '" +
											variableAccess.getVariableName() + "' for task " + ti);
									continue;
								}

								Object variable = ci.getVariable(variableAccess.getVariableName());
								variables.put(variableAccess.getVariableName(), variable);
							}
						}

						if (variableAccess.isWritable() && !variableAccess.isReadable()) {
							// we don't want to show non readable variable, this is backward compatibility, for task instances, that were created
							// with wrong process definition
							LOGGER.info("Variable '" + varName + "' is writable but not readable for task instance ID: " + ti.getId());
							variables.remove(variableAccess.getVariableName());
						}
					}
				}

				return variables;
			}
		});
	}

	@Override
	public List<BinaryVariable> resolveBinaryVariables(long taskInstanceId) {
		Map<String, Object> variables = populateVariables(taskInstanceId);
		return getBinaryVariablesHandler().resolveBinaryVariablesAsList(taskInstanceId, variables);
	}

	@Override
	public List<BinaryVariable> resolveBinaryVariables(long taskInstanceId, Variable variable) {
		Map<String, Object> variables = populateVariables(taskInstanceId);
		List<BinaryVariable> binVars = getBinaryVariablesHandler().resolveBinaryVariablesAsList(taskInstanceId, variables);

		if (ListUtil.isEmpty(binVars))
			return binVars;

		for (Iterator<BinaryVariable> iterator = binVars.iterator(); iterator.hasNext();) {
			BinaryVariable binaryVariable = iterator.next();

			if (!variable.getDefaultStringRepresentation().equals(binaryVariable.getVariable().getDefaultStringRepresentation())) {
				iterator.remove();
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

	@Override
	public BinaryVariablesHandler getBinaryVariablesHandler() {
		return binaryVariablesHandler;
	}

	@Autowired
	public void setBinaryVariablesHandler(BinaryVariablesHandler binaryVariablesHandler) {
		this.binaryVariablesHandler = binaryVariablesHandler;
	}

	@Override
	public void submitVariables(JbpmContext context, Map<String, Object> variables, long taskInstanceId, boolean validate) {
		submitVariables(context, variables, taskInstanceId, null, validate);
	}

	@Override
	@Transactional(readOnly = false)
	public void submitVariables(JbpmContext context, Map<String, Object> variables, long taskInstanceId, Long piId, boolean validate) {
		try {
			TaskInstance ti = context.getTaskInstance(taskInstanceId);
			if (ti.hasEnded()) {
				throw new TaskInstanceVariablesException("Task instance has already ended");
			}

			Task task = ti.getTask();
			TaskController tiController = task.getTaskController();

			if (tiController == null) {
				LOGGER.warning("No controller is assigned for task: " + task.getId() + ", task instance ID: " + ti.getId() + " - can not submit variables");
				// this occurs when no controller specified for the task
				return;
			}

			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = tiController.getVariableAccesses();

			if (variables == null) {
				variables = new HashMap<String, Object>();
			}
			Set<String> undeclaredVariables = MapUtil.isEmpty(variables) ? new HashSet<String>() : new HashSet<String>(variables.keySet());

			for (VariableAccess variableAccess: variableAccesses) {
				if (variableAccess == null) {
					continue;
				}

				String variableName = variableAccess.getVariableName();
				if (validate) {
					if (variableAccess.getAccess().isRequired() && !variables.containsKey(variableName))
						throw new TaskInstanceVariablesException("Required variable (" + variableName + ") not submitted.");

					if (!variableAccess.isWritable() && variables.containsKey(variableName)) {
						LOGGER.warning("Tried to submit read-only variable (" + variableAccess.getVariableName() + ") for task instance: " +
								taskInstanceId + ", ignoring.");
						variables.remove(variableName);
					}
				}

				undeclaredVariables.remove(variableName);
			}

			final Map<String, Object> variablesToSubmit = getBinaryVariablesHandler().storeBinaryVariables(taskInstanceId, variables);

			for (String undeclaredVariable: undeclaredVariables) {
				ti.setVariableLocally(undeclaredVariable, variablesToSubmit.get(undeclaredVariable));
			}

			setVariables(ti, variablesToSubmit);

			ProcessInstance pi = piId == null ? ti.getProcessInstance() : null;
			Long piIdToManage = piId == null ? pi == null ? null : pi.getId() : piId;
			doManageVariables(piIdToManage, ti.getId(), variables);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error submitting variables " + variables + " for task instance: " + taskInstanceId, e);
		}
	}
}