package com.idega.jbpm.variables;

import java.io.InputStream;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 13:52:39 $ by $Author: civilis $
 */
public interface BinaryVariablesHandler {

	/**
	 * checks variables for binary variables types (e.g. file and files data type), stores them to some persistence (e.g. slide),
	 * puts the identifiers in the variables values, instead of File object(s)
	 * @param identifier - any identifier, usually task instance id
	 * @param variables
	 * @return new map with binary variables values changed to string identifier(s)
	 */
	public abstract Map<String, Object> storeBinaryVariables(Object identifier,
			Map<String, Object> variables);

	public abstract InputStream getBinaryVariableContent(BinaryVariable variable);
	
	public abstract Map<String, Object> resolveBinaryVariables(
			Map<String, Object> variables);
	
	public abstract List<BinaryVariable> resolveBinaryVariablesAsList(Map<String, Object> variables);
}