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
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.graph.exe.Token;
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
import com.idega.jbpm.variables.BinaryVariable;
import com.idega.jbpm.variables.BinaryVariablesHandler;
import com.idega.jbpm.variables.VariablesHandler;
import com.idega.jbpm.variables.VariablesManager;
import com.idega.util.datastructures.map.MapUtil;

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
		if (originalVariables == null || originalVariables.isEmpty())
			return null;

		return getIdegaJbpmContext().execute(new JbpmCallback<Map<String, Object>>() {

			@Override
			public Map<String, Object> doInJbpm(JbpmContext context) throws JbpmException {
				final Map<String, Object> variables = getBinaryVariablesHandler().storeBinaryVariables(taskInstanceId, originalVariables);

				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				Token taskInstanceToken = ti.getToken();

				@SuppressWarnings("unchecked")
				Map<String, VariableInstance> varInstances = ti.getVariableInstances();

				for (Entry<String, Object> entry : variables.entrySet()) {
					if (varInstances != null && !varInstances.containsKey(entry.getKey())) {
						VariableInstance vi = VariableInstance.create(taskInstanceToken, entry.getKey(), entry.getValue());
						ti.addVariableInstance(vi);
					}
				}

				@SuppressWarnings("unchecked")
				Map<String, Object> vars = ti.getVariables();
				vars.putAll(variables);
				setVariables(ti, vars);

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

			@Override
			public Map<String, Object> doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance ti = context.getTaskInstance(taskInstanceId);

				@SuppressWarnings("unchecked")
				Map<String, Object> variables = new HashMap<String, Object>(ti.getVariablesLocally());

				if (!ti.hasEnded()) {
					@SuppressWarnings("unchecked")
					List<VariableAccess> accesses = ti.getTask().getTaskController().getVariableAccesses();

					for (VariableAccess variableAccess : accesses) {
						if (!variables.containsKey(variableAccess.getVariableName())) {
							// the situation when process definition was changed (using formbuilder for instance) but task instance was created already
							if (variableAccess.isReadable()) {
								// read - populating from token, TODO: test if this populates variable from the token
								Object variable = ti.getContextInstance().getVariable(variableAccess.getVariableName());
								variables.put(variableAccess.getVariableName(), variable);
							}
						}

						if (variableAccess.isWritable() && !variableAccess.isReadable()) {
							// we don't want to show non readable variable, this is backward compatibility, for task instances, that were created
							// with wrong process definition
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
		return getBinaryVariablesHandler().resolveBinaryVariablesAsList(variables);
	}

	@Override
	public List<BinaryVariable> resolveBinaryVariables(long taskInstanceId, Variable variable) {
		Map<String, Object> variables = populateVariables(taskInstanceId);
		List<BinaryVariable> binVars = getBinaryVariablesHandler().resolveBinaryVariablesAsList(variables);

		if (binVars == null)
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
	public void setBinaryVariablesHandler(
	        BinaryVariablesHandler binaryVariablesHandler) {
		this.binaryVariablesHandler = binaryVariablesHandler;
	}

	@Override
	@Transactional(readOnly = false)
	public void submitVariables(JbpmContext context, Map<String, Object> variables, long taskInstanceId, boolean validate) {
		try {
			TaskInstance ti = context.getTaskInstance(taskInstanceId);
			if (ti.hasEnded())
				throw new TaskInstanceVariablesException("Task instance has already ended");

			TaskController tiController = ti.getTask().getTaskController();

			if (tiController == null)
				// this occurs when no controller specified for the task
				return;

			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = tiController.getVariableAccesses();

			Set<String> undeclaredVariables = new HashSet<String>(variables.keySet());

			for (VariableAccess variableAccess : variableAccesses) {
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

			context.getSession().flush();

			doManageVariables(ti.getProcessInstance().getId(), ti.getId(), variables);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error submitting variables " + variables + " for task instance: " + taskInstanceId, e);
		}
		return;
	}
}