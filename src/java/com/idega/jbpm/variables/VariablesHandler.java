package com.idega.jbpm.variables;

import java.util.List;
import java.util.Map;

import com.idega.block.process.variables.Variable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/10/13 13:32:12 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submitVariables(Map<String, Object> variables, long taskInstanceId, boolean validate);
	
	public abstract Map<String, Object> submitVariablesExplicitly(Map<String, Object> variables, long taskInstanceId);

	public abstract Map<String, Object> populateVariables(long taskInstanceId);
	
	public abstract List<BinaryVariable> resolveBinaryVariables(long taskInstanceId);
	
	public abstract List<BinaryVariable> resolveBinaryVariables(long taskInstanceId, Variable variable);
	
	public abstract BinaryVariablesHandler getBinaryVariablesHandler();
	
}