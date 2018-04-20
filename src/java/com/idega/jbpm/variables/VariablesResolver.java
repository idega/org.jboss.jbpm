package com.idega.jbpm.variables;

import java.util.List;
import java.util.logging.Level;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Service("bpmVariableResolver")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariablesResolver extends DefaultSpringBean {

	private ThreadLocal<ExecutionContext> executionContextLocal = new ThreadLocal<ExecutionContext>();

	@Autowired(required = false)
	private BPMDAO bpmDAO;

	@Autowired(required = false)
	private BPMContext bpmContext;

	public void setExecutionContext(ExecutionContext ectx) {
		executionContextLocal.set(ectx);
	}

	/**
	 * Returns a variable with that matching name
	 *
	 * @param variableName
	 * @return
	 */
	public Object get(String variableName) {
		ExecutionContext ectx = executionContextLocal.get();
		if (ectx != null) {
			Object value = ectx.getContextInstance().getVariable(variableName, ectx.getToken());

			if (value == null) {
				value = ectx.getVariable(variableName);
			}

			if (value == null && ectx.getProcessInstance() != null) {
				value = getFromParentProcess(ectx.getProcessInstance(), variableName);
			} else if (value == null) {
				getLogger().warning("Can not resolve value for '" + variableName + "' because proc. inst. is unknown in execution context " + ectx);
			}

			return value;
		} else {
			getLogger().warning("Execution context was not set before resolving variable: " + variableName);
			return null;
		}
	}

	private Object getFromParentProcess(ProcessInstance pi, String variableName) {
		if (pi == null || StringUtil.isEmpty(variableName)) {
			return null;
		}

		try {
			if (bpmDAO == null || bpmContext == null) {
				ELUtil.getInstance().autowire(this);
			}

			List<ProcessInstance> rootProcInstances = bpmDAO.getRootProcesses(pi.getId());
			if (ListUtil.isEmpty(rootProcInstances)) {
				getLogger().warning("There are no root process instances for current process instance (" + pi + "), can not resolve value for variable " + variableName);
				return null;
			}

			return bpmContext.execute(new JbpmCallback<Object>() {

				@Override
				public Object doInJbpm(JbpmContext context) throws JbpmException {
					for (ProcessInstance rootProcInstance: rootProcInstances) {
						ProcessInstance rootPi = null;
						try {
							rootPi = context.getProcessInstance(rootProcInstance.getId());
							Object value = rootPi.getContextInstance().getVariable(variableName);
							if (value != null) {
								return value;
							}
						} catch (Exception e) {
							getLogger().log(Level.WARNING, "Error getting value for variable " + variableName + " from root process instance " + rootPi + ", current process instance: " + pi, e);
						}
					}

					return null;
				}

			});
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting variable '" + variableName + "' from parent process instance. Current process instance: " + pi, e);
		}
		return null;
	}

	/**
	 *
	 * @param beanName
	 *            just name (not an expression)
	 * @return bean from Spring or JSF context
	 */
	public Object bean(String beanName) {
		return ELUtil.getInstance().getBean(beanName);
	}

}