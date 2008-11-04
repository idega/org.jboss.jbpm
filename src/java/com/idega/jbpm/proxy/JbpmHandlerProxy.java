package com.idega.jbpm.proxy;

import java.util.HashMap;
import java.util.Map;

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

public class JbpmHandlerProxy implements ActionHandler, AssignmentHandler, DecisionHandler, TaskControllerHandler{


	/**
	 * 
	 */
	private static final long serialVersionUID = 6814826999336517269L;

	/**
	 *  Properties to be set for handler class
	 */
	private Map<String, String> propertyMap;
	
	/**
	 * Name of a handler 
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


	public void execute(ExecutionContext ectx) throws Exception {
		
	//	System.out.println("___________________JbpmHandlerProxy called, bean name: " + handlerName);
		
		ActionHandler handler = ELUtil.getInstance().getBean(handlerName);
		Map<String, Object> nonExistingProperties = null;
		if(getPropertyMap() != null){
			
	//		System.out.println("____________________________we have some properties!");
			
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(handler);
			
			
			for(String propertyName:getPropertyMap().keySet()){
				Object propValue;
				
				if(getPropertyMap().get(propertyName).startsWith("#{") 
						&& getPropertyMap().get(propertyName).endsWith("}")){
	
					propValue = JbpmExpressionEvaluator.evaluate(getPropertyMap().get(propertyName), ectx);
				}else{
					propValue = getPropertyMap().get(propertyName);		
				}
				
				try{
					wrapper.setPropertyValue(propertyName, propValue);
				//if property doesn't exist:
				}catch (NotWritablePropertyException e) {
					if (nonExistingProperties == null){
						nonExistingProperties = new HashMap<String, Object>();
					}
					nonExistingProperties.put(propertyName, propValue);
				}
			}
		}
		nonExistingProperties =  new HashMap<String, Object>();
		if(nonExistingProperties == null){
			handler.execute(ectx);
		}else{
			try{
				((ParamActionHandler) handler).execute(ectx, nonExistingProperties);
			}catch (ClassCastException e) {
				// TODO: Someone who coded made An Error, add to other methods to!
			}
		}
	}

	public void assign(Assignable ass, ExecutionContext ectx) throws Exception {
		
		//System.out.println("___________________JbpmHandlerProxy called, bean name: " + handlerName);
		
		AssignmentHandler handler = ELUtil.getInstance().getBean(handlerName);
		Map<String, Object> nonExistingProperties = null;
		if(getPropertyMap() != null){
		//	System.out.println("____________________________we have some properties!");
			
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(handler);
			
			
			for(String propertyName:getPropertyMap().keySet()){
				Object propValue;
				
				if(getPropertyMap().get(propertyName).startsWith("#{") 
						&& getPropertyMap().get(propertyName).endsWith("}")){
	
					propValue = JbpmExpressionEvaluator.evaluate(getPropertyMap().get(propertyName), ectx);
				}else{
					propValue = getPropertyMap().get(propertyName);		
				}
				
				try{
					wrapper.setPropertyValue(propertyName, propValue);
				//if property doesn't exist:
				}catch (NotWritablePropertyException e) {
					if (nonExistingProperties == null){
						nonExistingProperties = new HashMap<String, Object>();
					}
					nonExistingProperties.put(propertyName, propValue);
				}
			}
		}
		if(nonExistingProperties == null){
			handler.assign(ass, ectx);
		}else{
			((ParamAssignmentHandler) handler).assign(ass, ectx, nonExistingProperties);
		}
		
		
	}

	public String decide(ExecutionContext ectx) throws Exception {
		
	//	System.out.println("___________________JbpmHandlerProxy called, bean name: " + handlerName);
		
		DecisionHandler handler = ELUtil.getInstance().getBean(handlerName);
		Map<String, Object> nonExistingProperties = null;
		if(getPropertyMap() != null){
	//		System.out.println("____________________________we have some properties!");
			
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(handler);
			
			
			for(String propertyName:getPropertyMap().keySet()){
				Object propValue;
				
				if(getPropertyMap().get(propertyName).startsWith("#{") 
						&& getPropertyMap().get(propertyName).endsWith("}")){
	
					propValue = JbpmExpressionEvaluator.evaluate(getPropertyMap().get(propertyName), ectx);
				}else{
					propValue = getPropertyMap().get(propertyName);		
				}
				
				try{
					wrapper.setPropertyValue(propertyName, propValue);
				//if property doesn't exist:
				}catch (NotWritablePropertyException e) {
					if (nonExistingProperties == null){
						nonExistingProperties = new HashMap<String, Object>();
					}
					nonExistingProperties.put(propertyName, propValue);
				}
			}
		}		
		if(nonExistingProperties == null){
			return handler.decide(ectx);
		}else{
			return ((ParamDecisionHandler) handler).decide(ectx, nonExistingProperties);
		}
	}

	public void initializeTaskVariables(TaskInstance arg0,
			ContextInstance arg1, Token arg2) {
	//	System.out.println("___________________JbpmHandlerProxy called, bean name: " + handlerName);
		
		TaskControllerHandler handler = ELUtil.getInstance().getBean(handlerName);
		Map<String, Object> nonExistingProperties = null;		
		if(getPropertyMap() != null){
	//		System.out.println("____________________________we have some properties!");
			
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(handler);
			
			
			for(String propertyName:getPropertyMap().keySet()){
				Object propValue;
				
				if(getPropertyMap().get(propertyName).startsWith("#{") 
						&& getPropertyMap().get(propertyName).endsWith("}")){
	
					propValue = JbpmExpressionEvaluator.evaluate(getPropertyMap().get(propertyName), new ExecutionContext(arg2));
				}else{
					propValue = getPropertyMap().get(propertyName);		
				}
				
				try{
					wrapper.setPropertyValue(propertyName, propValue);
				//if property doesn't exist:
				}catch (NotWritablePropertyException e) {
					if (nonExistingProperties == null){
						nonExistingProperties = new HashMap<String, Object>();
					}
					nonExistingProperties.put(propertyName, propValue);
				}
			}
		}		
		if(nonExistingProperties == null){
			handler.initializeTaskVariables(arg0, arg1, arg2);
		}else{
			((ParamTaskControllerHandler) handler).initializeTaskVariables(arg0, arg1, arg2, nonExistingProperties);
		}
	}

	public void submitTaskVariables(TaskInstance arg0, ContextInstance arg1,
			Token arg2) {
	//	System.out.println("___________________JbpmHandlerProxy called, bean name: " + handlerName);
		
		TaskControllerHandler handler = ELUtil.getInstance().getBean(handlerName);
		Map<String, Object> nonExistingProperties = null;
		if(getPropertyMap() != null){
	//		System.out.println("____________________________we have some properties!");
			
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(handler);
			
			
			for(String propertyName:getPropertyMap().keySet()){
				Object propValue;
				
				if(getPropertyMap().get(propertyName).startsWith("#{") 
						&& getPropertyMap().get(propertyName).endsWith("}")){
	
					propValue = JbpmExpressionEvaluator.evaluate(getPropertyMap().get(propertyName), new ExecutionContext(arg2));
				}else{
					propValue = getPropertyMap().get(propertyName);		
				}
				
				try{
					wrapper.setPropertyValue(propertyName, propValue);
				//if property doesn't exist:
				}catch (NotWritablePropertyException e) {
					if (nonExistingProperties == null){
						nonExistingProperties = new HashMap<String, Object>();
					}
					nonExistingProperties.put(propertyName, propValue);
				}
			}
		}		
		if(nonExistingProperties == null){
			handler.submitTaskVariables(arg0, arg1, arg2);
		}else {
			((ParamTaskControllerHandler) handler).submitTaskVariables(arg0, arg1, arg2, nonExistingProperties);
		}
	}

}
