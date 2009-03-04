package com.idega.jbpm.events;

import java.util.Collection;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * fires event by name provided to the process by processInstanceId provided. if
 * variablesToPopoulate is provided, values are taken from context instance <b>depending on
 * variableScope value provided</b>. So if you want to populate variables, make sure they are
 * present on the scope presented. By default global scope is used (as this is jbpm default -
 * globality). Scopes supported: global, local
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/03/04 14:11:06 $ by $Author: civilis $
 */
@Service("fireProcessEventHandler")
@Scope("prototype")
public class FireProcessEventHandler implements ActionHandler {
	
	private static final long serialVersionUID = 8565645037795585170L;
	
	private Long processInstanceId;
	private String eventName;
	private Collection<String> variablesToPopoulate;
	private String variablesScope;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		if (getProcessInstanceId() == null)
			throw new IllegalArgumentException(
			        "No process instance id provided");
		
		String eventName = getEventName();
		
		if (StringUtil.isEmpty(eventName))
			throw new IllegalArgumentException("No event name provided");
		
		ProcessInstance pi = ectx.getJbpmContext().getProcessInstance(
		    getProcessInstanceId());
		
		Token newToken = new Token(pi.getRootToken(), eventName
		        + CoreConstants.UNDER + System.currentTimeMillis());
		
		ExecutionContext newEctx = new ExecutionContext(newToken);
		
		if (!ListUtil.isEmpty(getVariablesToPopoulate())) {
			
			// populating variables from current token to new one
			
			Token token = ectx.getToken();
			ContextInstance contextInstance = ectx.getContextInstance();
			ContextInstance newContextInstance = newEctx.getContextInstance();
			
			// we support only two scopes now - global and local
			boolean globalScope;
			
			if ("global".equals(getVariablesScope())) {
				
				globalScope = true;
				
			} else if ("local".equals(getVariablesScope())) {
				
				globalScope = false;
				
			} else {
				throw new IllegalArgumentException(
				        "Unsupported variable scope variable = "
				                + getVariablesScope()
				                + ". Only global and local scopes are supported");
			}
			
			for (String variableName : getVariablesToPopoulate()) {
				
				Object variableValue;
				
				if (globalScope) {
					variableValue = contextInstance.getVariable(variableName,
					    token);
					
				} else {
					variableValue = contextInstance.getVariableLocally(
					    variableName, token);
				}
				
				newContextInstance.setVariableLocally(variableName,
				    variableValue, newToken);
			}
		}
		
		pi.getProcessDefinition().fireEvent(eventName, newEctx);
	}
	
	public Long getProcessInstanceId() {
		return processInstanceId;
	}
	
	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}
	
	public Collection<String> getVariablesToPopoulate() {
		return variablesToPopoulate;
	}
	
	public void setVariablesToPopoulate(Collection<String> variablesToPopoulate) {
		this.variablesToPopoulate = variablesToPopoulate;
	}
	
	public String getEventName() {
		return eventName;
	}
	
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	
	public String getVariablesScope() {
		
		return variablesScope != null ? variablesScope : "global";
	}
	
	public void setVariablesScope(String variablesScope) {
		this.variablesScope = variablesScope;
	}
}