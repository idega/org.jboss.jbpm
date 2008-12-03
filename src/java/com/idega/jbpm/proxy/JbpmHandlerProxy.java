package com.idega.jbpm.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.def.TaskControllerHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;

import com.idega.jbpm.handler.ParamActionHandler;
import com.idega.jbpm.handler.ParamAssignmentHandler;
import com.idega.jbpm.handler.ParamDecisionHandler;
import com.idega.jbpm.handler.ParamTaskControllerHandler;
import com.idega.util.expression.ELUtil;

/**
 * 
 * This class is a proxy between jbmp handlers and spring.
 * 
 * It should be used in jpdl when declaring any handler, name of a handler
 * should be passed as a parameter "handlerName".
 * 
 * All parameters for concrete handler should be passed as a parameter map
 * "propertyMap": <properteName, properteValue> where value can be string,
 * expression( #{} ) or a beanshell script ( ${} ), which must return object.
 * 
 * 
 * @author juozas
 * 
 */
public class JbpmHandlerProxy implements ActionHandler, AssignmentHandler,
		DecisionHandler, TaskControllerHandler {

	private static final long serialVersionUID = -5175514284127589012L;

	/**
	 * Properties to be set for handler class
	 */
	private Map<String, String> propertyMap;

	/**
	 * Name of a handler that should be used
	 */
	private String handlerName;

	public String getHandlerName() {
		return handlerName;
	}

	public void setHandlerName(String actionHandlerName) {
		this.handlerName = actionHandlerName;
	}

	public Map<String, String> getPropertyMap() {
		return propertyMap;
	}

	public void setPropertyMap(Map<String, String> propertyMap) {
		this.propertyMap = propertyMap;
	}

	protected Object getHandler(ExecutionContext ectx) {
		Object handler = ELUtil.getInstance().getBean(getHandlerName());
		
		if(handler == null){
			setHandlerName((String) JbpmExpressionEvaluator.evaluate(getHandlerName(), ectx));
			handler = ELUtil.getInstance().getBean(getHandlerName());
		}
		
		return handler;
	}

	public void execute(ExecutionContext ectx) throws Exception {

		Object handler = getHandler(ectx);

		if (handler == null || !(handler instanceof ActionHandler)) {

			Logger.getLogger(getClass().getName()).log(
					Level.SEVERE,
					"Handler proxy called, but no handler, or wrong handler type ("
							+ handler == null ? null : handler.getClass()
							.getName()
							+ ") resolved by handler name="
							+ getHandlerName()
							+ ". Process definition id="
							+ ectx.getProcessDefinition().getId());
		}

		Map<String, Object> nonExistingProperties = injectProperties(handler,
				ectx);

		if (nonExistingProperties == null
				|| !(handler instanceof ParamActionHandler)) {

			((ActionHandler) handler).execute(ectx);

		} else {

			((ParamActionHandler) handler).execute(ectx, nonExistingProperties);
		}
	}

	public void assign(Assignable ass, ExecutionContext ectx) throws Exception {

		Object handler = getHandler(ectx);

		if (handler == null || !(handler instanceof AssignmentHandler)) {

			Logger.getLogger(getClass().getName()).log(
					Level.SEVERE,
					"Handler proxy called, but no handler, or wrong handler type ("
							+ handler == null ? null : handler.getClass()
							.getName()
							+ ") resolved by handler name="
							+ getHandlerName()
							+ ". Process definition id="
							+ ectx.getProcessDefinition().getId());
		}

		Map<String, Object> nonExistingProperties = injectProperties(handler,
				ectx);

		if (nonExistingProperties == null
				|| !(handler instanceof ParamAssignmentHandler)) {

			((AssignmentHandler) handler).assign(ass, ectx);

		} else {

			((ParamAssignmentHandler) handler).assign(ass, ectx,
					nonExistingProperties);
		}
	}

	public String decide(ExecutionContext ectx) throws Exception {

		Object handler = getHandler(ectx);

		if (handler == null || !(handler instanceof DecisionHandler)) {

			Logger.getLogger(getClass().getName()).log(
					Level.SEVERE,
					"Handler proxy called, but no handler, or wrong handler type ("
							+ handler == null ? null : handler.getClass()
							.getName()
							+ ") resolved by handler name="
							+ getHandlerName()
							+ ". Process definition id="
							+ ectx.getProcessDefinition().getId());
		}

		Map<String, Object> nonExistingProperties = injectProperties(handler,
				ectx);

		if (nonExistingProperties == null
				|| !(handler instanceof ParamDecisionHandler)) {

			return ((DecisionHandler) handler).decide(ectx);

		} else {

			return ((ParamDecisionHandler) handler).decide(ectx,
					nonExistingProperties);
		}
	}

	public void initializeTaskVariables(TaskInstance ti,
			ContextInstance contextInstance, Token tkn) {

		
		ExecutionContext ectx = new ExecutionContext(tkn);
		Object handler = getHandler(ectx);
		if (handler == null || !(handler instanceof TaskControllerHandler)) {

			Logger.getLogger(getClass().getName()).log(
					Level.SEVERE,
					"Handler proxy called, but no handler, or wrong handler type ("
							+ handler == null ? null : handler.getClass()
							.getName()
							+ ") resolved by handler name="
							+ getHandlerName()
							+ ". Process definition id="
							+ ectx.getProcessDefinition().getId());
		}

		Map<String, Object> nonExistingProperties = injectProperties(handler,
				ectx);

		if (nonExistingProperties == null
				|| !(handler instanceof ParamTaskControllerHandler)) {

			((TaskControllerHandler) handler).initializeTaskVariables(ti,
					contextInstance, tkn);

		} else {

			((ParamTaskControllerHandler) handler).initializeTaskVariables(ti,
					contextInstance, tkn, nonExistingProperties);
		}
	}

	public void submitTaskVariables(TaskInstance ti,
			ContextInstance contextInstance, Token tkn) {

		
		ExecutionContext ectx = new ExecutionContext(tkn);
		Object handler = getHandler(ectx);
		if (handler == null || !(handler instanceof TaskControllerHandler)) {

			Logger.getLogger(getClass().getName()).log(
					Level.SEVERE,
					"Handler proxy called, but no handler, or wrong handler type ("
							+ handler == null ? null : handler.getClass()
							.getName()
							+ ") resolved by handler name="
							+ getHandlerName()
							+ ". Process definition id="
							+ ectx.getProcessDefinition().getId());
		}

		Map<String, Object> nonExistingProperties = injectProperties(handler,
				ectx);

		if (nonExistingProperties == null
				|| !(handler instanceof ParamTaskControllerHandler)) {

			((TaskControllerHandler) handler).submitTaskVariables(ti,
					contextInstance, tkn);

		} else {

			((ParamTaskControllerHandler) handler).submitTaskVariables(ti,
					contextInstance, tkn, nonExistingProperties);
		}
	}

	private Map<String, Object> injectProperties(Object handler,
			ExecutionContext ectx) {

		Map<String, Object> nonExistingProperties = null;

		if (getPropertyMap() != null) {

			BeanWrapper wrapper = PropertyAccessorFactory
					.forBeanPropertyAccess(handler);

			for (Entry<String, String> property : getPropertyMap().entrySet()) {

				final String propertyName = property.getKey();
				final String propertyValueExp = property.getValue();
				final Object propertyValue;

				if (propertyValueExp.startsWith("#{")
						&& propertyValueExp.endsWith("}")) {

					propertyValue = JbpmExpressionEvaluator.evaluate(
							getPropertyMap().get(propertyName), ectx);

				} else if (propertyValueExp.startsWith("${")
						&& propertyValueExp.endsWith("}")) {

					String script = propertyValueExp.substring(2,
							propertyValueExp.length() - 1);

					try {
						propertyValue = ScriptEvaluator.evaluate(script, ectx);

					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				} else {
					propertyValue = propertyValueExp;
				}

				try {
					wrapper.setPropertyValue(propertyName, propertyValue);

				} catch (NotWritablePropertyException e) {
					// if property doesn't exist:
					if (nonExistingProperties == null) {
						nonExistingProperties = new HashMap<String, Object>();
					}
					nonExistingProperties.put(propertyName, propertyValue);
				}
			}
		}

		return nonExistingProperties;
	}
}