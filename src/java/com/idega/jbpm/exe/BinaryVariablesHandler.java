package com.idega.jbpm.exe;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.idega.jbpm.exe.impl.BinaryVariable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/29 20:28:24 $ by $Author: civilis $
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