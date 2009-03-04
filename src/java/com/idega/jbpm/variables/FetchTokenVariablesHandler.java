package com.idega.jbpm.variables;

import java.util.Map;
import java.util.Map.Entry;

import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * fetches variables from the token provided, and puts them to the executin context. variablesScope
 * provides, to what scope the variables should be <b>set</b> - local or global. Designating local
 * points out to set variables locally for token, else, simple setVariable for token is used
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/04 14:11:16 $ by $Author: civilis $
 */
@Service("fetchTokenVariables")
@Scope("prototype")
public class FetchTokenVariablesHandler implements ActionHandler {
	
	private static final long serialVersionUID = -2643943042204471758L;
	
	private String variablesScope;
	private Long tokenId;
	
	@SuppressWarnings("unchecked")
	public void execute(ExecutionContext ectx) throws Exception {
		
		Long tokenToFetchId = getTokenId();
		
		if (tokenToFetchId == null)
			throw new IllegalArgumentException("No token id provided");
		
		Token tokenToFetch = ectx.getJbpmContext().getToken(tokenToFetchId);
		ContextInstance contextInstanceToFetch = tokenToFetch
		        .getProcessInstance().getContextInstance();
		
		Map<String, Object> fetchedVariables = contextInstanceToFetch
		        .getVariables(tokenToFetch);
		
		ContextInstance contextInstace = ectx.getContextInstance();
		Token token = ectx.getToken();
		
		if ("global".equals(getVariablesScope())) {
			
			for (Entry<String, Object> variableEntry : fetchedVariables
			        .entrySet()) {
				
				contextInstace.setVariable(variableEntry.getKey(),
				    variableEntry.getValue(), token);
			}
			
		} else if ("local".equals(getVariablesScope())) {
			
			for (Entry<String, Object> variableEntry : fetchedVariables
			        .entrySet()) {
				
				contextInstace.setVariableLocally(variableEntry.getKey(),
				    variableEntry.getValue(), token);
			}
			
		} else {
			throw new IllegalArgumentException(
			        "Unsupported variable scope variable = "
			                + getVariablesScope()
			                + ". Only global and local scopes are supported");
		}
	}
	
	public String getVariablesScope() {
		return variablesScope != null ? variablesScope : "global";
	}
	
	public void setVariablesScope(String variablesScope) {
		this.variablesScope = variablesScope;
	}
	
	public Long getTokenId() {
		return tokenId;
	}
	
	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}
}