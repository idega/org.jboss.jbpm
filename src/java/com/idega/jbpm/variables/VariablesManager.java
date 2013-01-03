package com.idega.jbpm.variables;

import java.util.Map;

public interface VariablesManager {

	public void doManageVariables(Long processInstanceId, Long taskInstanceId, Map<String, Object> variables);
	
}