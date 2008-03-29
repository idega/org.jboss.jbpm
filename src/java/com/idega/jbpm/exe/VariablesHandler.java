package com.idega.jbpm.exe;

import java.util.List;
import java.util.Map;

import com.idega.jbpm.exe.impl.BinaryVariable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.9 $
 *
 * Last modified: $Date: 2008/03/29 20:28:24 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submitVariables(Map<String, Object> variables,
			long taskInstanceId, boolean validate);

	public abstract Map<String, Object> populateVariables(long taskInstanceId);
	
	public abstract List<BinaryVariable> resolveBinaryVariables(long taskInstanceId);
	
	public abstract BinaryVariablesHandler getBinaryVariablesHandler();
}