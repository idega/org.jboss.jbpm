package com.idega.jbpm.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.handler.ParamActionHandler;
import com.idega.jbpm.handler.ParamAssignmentHandler;
import com.idega.jbpm.handler.ParamDecisionHandler;
import com.idega.jbpm.handler.ParamTaskControllerHandler;
import com.idega.util.expression.ELUtil;

/**
 * This class is a proxy between jbmp handlers and spring. It should be used in jpdl when declaring
 * any handler, name of a handler should be passed as a parameter "handlerName". All parameters for
 * concrete handler should be passed as a parameter map "propertyMap": <propertyName, propertyValue>
 * where value can be string, expression( #{} ) or a beanshell script ( ${} ), which must return
 * object.
 *
 * @author juozas
 */
public class JbpmHandlerProxy implements ActionHandler, AssignmentHandler,
        DecisionHandler, TaskControllerHandler {

	private static final long serialVersionUID = -5175514284127589012L;

	/**
	 * Properties to be set for handler class
	 */
	private Map<String, String> propertyMap;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private ScriptEvaluator scriptEvaluator;

	/**
	 * Name of a handler that should be used. Could be variable expression e.g.
	 * #{myVarOfHandlerName}
	 */
	private String handlerName;

	/**
	 * expression, or true and false values
	 */
	private String async;

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

		String handlerName = (String) JbpmExpressionEvaluator.evaluate(
		    getHandlerName(), ectx);

		if (handlerName == null) {

			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "No handler name resolved/provided for token id = "
			            + ectx.getToken().getId()
			            + " by handler name or expression provided = "
			            + getHandlerName());

			return null;
		} else {

			return ELUtil.getInstance().getBean(handlerName);
		}
	}

	@Override
	public void execute(final ExecutionContext ectx) throws Exception {

		final Object handler = getHandler(ectx);

		if (handler == null || !(handler instanceof ActionHandler)) {

			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Handler proxy called, but no handler, or wrong handler type ("
			            + handler == null ? null : handler.getClass().getName()
			            + ") resolved by handler name=" + getHandlerName()
			            + ". Process definition id="
			            + ectx.getProcessDefinition().getId());
		}

		final Map<String, Object> nonExistingProperties = injectProperties(
		    handler, ectx);

		if (isAsync(ectx)) {
			// async execution of action handler

			// TODO: change implementation - add to job executor jobs

			new Thread(new Runnable() {

				@Override
				public void run() {

					getBpmContext().execute(new JbpmCallback() {

						@Override
						public Object doInJbpm(JbpmContext context)
						        throws JbpmException {

							// TODO: this is probably not a valid execution context - i.e. it needs
							// to be merged in the transaction

							try {
								if (nonExistingProperties == null
								        || !(handler instanceof ParamActionHandler)) {

									((ActionHandler) handler).execute(ectx);

								} else {

									((ParamActionHandler) handler).execute(
									    ectx, nonExistingProperties);
								}

							} catch (Exception e) {
								Logger
								        .getLogger(getClass().getName())
								        .log(
								            Level.SEVERE,
								            "Exception while executing action handler in async mode",
								            e);
							}
							return null;
						}
					});
				}
			}).start();

		} else {
			if (nonExistingProperties == null
			        || !(handler instanceof ParamActionHandler)) {

				((ActionHandler) handler).execute(ectx);

			} else {

				((ParamActionHandler) handler).execute(ectx,
				    nonExistingProperties);
			}
		}
	}

	@Override
	public void assign(Assignable ass, ExecutionContext ectx) throws Exception {

		Object handler = getHandler(ectx);

		if (handler == null || !(handler instanceof AssignmentHandler)) {

			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Handler proxy called, but no handler, or wrong handler type ("
			            + handler == null ? null : handler.getClass().getName()
			            + ") resolved by handler name=" + getHandlerName()
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

	@Override
	public String decide(ExecutionContext ectx) throws Exception {

		Object handler = getHandler(ectx);

		if (handler == null || !(handler instanceof DecisionHandler)) {

			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Handler proxy called, but no handler, or wrong handler type ("
			            + handler == null ? null : handler.getClass().getName()
			            + ") resolved by handler name=" + getHandlerName()
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

	@Override
	public void initializeTaskVariables(TaskInstance ti,
	        ContextInstance contextInstance, Token tkn) {

		ExecutionContext ectx = new ExecutionContext(tkn);
		Object handler = getHandler(ectx);
		if (handler == null || !(handler instanceof TaskControllerHandler)) {

			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Handler proxy called, but no handler, or wrong handler type ("
			            + handler == null ? null : handler.getClass().getName()
			            + ") resolved by handler name=" + getHandlerName()
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

	@Override
	public void submitTaskVariables(TaskInstance ti,
	        ContextInstance contextInstance, Token tkn) {

		ExecutionContext ectx = new ExecutionContext(tkn);
		Object handler = getHandler(ectx);
		if (handler == null || !(handler instanceof TaskControllerHandler)) {

			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Handler proxy called, but no handler, or wrong handler type ("
			            + handler == null ? null : handler.getClass().getName()
			            + ") resolved by handler name=" + getHandlerName()
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

	private Object getPropertyValue(ExecutionContext ectx, String propertyValueExp) {
		final Object propertyValue;

		if (propertyValueExp.startsWith("#{") && propertyValueExp.endsWith("}")) {
			propertyValue = JbpmExpressionEvaluator.evaluate(propertyValueExp, ectx);

		} else if (propertyValueExp.startsWith("${") && propertyValueExp.endsWith("}")) {
			String script = propertyValueExp.substring(2, propertyValueExp.length() - 1);

			try {
				int jbpmExpressionStart = script.indexOf("#{");
				if (jbpmExpressionStart != -1) {
					String[] expressions = script.split("\\#\\{");
					for (int i = 0;i < expressions.length;i++) {
						String expression = expressions[i];
						int endIndex = expression.indexOf("}");
						if (((i == 0) && (jbpmExpressionStart != 0)) || (endIndex < 0)) {
							continue;
						}
						String variable = "#{" + expression.substring(0,endIndex+1);

						String regex = Pattern.quote(variable);
						script = script.replaceAll(regex, JbpmExpressionEvaluator.evaluate(variable,ectx).toString());
					}
				}
				propertyValue = getScriptEvaluator().evaluate(script, ectx);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			propertyValue = propertyValueExp;
		}

		return propertyValue;
	}

	private Map<String, Object> injectProperties(Object handler, ExecutionContext ectx) {
		Map<String, Object> nonExistingProperties = null;
		if (getPropertyMap() != null) {
			BeanWrapper wrapper = PropertyAccessorFactory .forBeanPropertyAccess(handler);
			for (Entry<String, String> property: getPropertyMap().entrySet()) {
				final String propertyName = property.getKey();
				final String propertyValueExp = property.getValue();
				final Object propertyValue = getPropertyValue(ectx, propertyValueExp);

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

	public static Object bean(String beanName) {
		return ELUtil.getInstance().getBean(beanName);
	}

	public String getAsync() {
		return async;
	}

	public boolean isAsync(ExecutionContext ectx) {

		//if(true)
			return false;

		/*if (StringUtil.isEmpty(getAsync()))
			return false;
		else {
			if ("true".equals(getAsync())) {
				return true;
			} else if ("false".equals(getAsync())) {
				return false;
			} else {

				Object propVal = getPropertyValue(ectx, getAsync());
				return Boolean.TRUE.equals(propVal);
			}
		}*/
	}

	public void setAsync(String async) {
		this.async = async;
	}

	BPMContext getBpmContext() {

		if (bpmContext == null)
			ELUtil.getInstance().autowire(this);

		return bpmContext;
	}

	ScriptEvaluator getScriptEvaluator() {

		if (scriptEvaluator == null)
			ELUtil.getInstance().autowire(this);

		return scriptEvaluator;
	}
}