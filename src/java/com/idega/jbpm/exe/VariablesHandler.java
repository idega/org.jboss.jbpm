package com.idega.jbpm.exe;

import java.util.Map;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/03/12 15:43:03 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submitVariables(Map<String, Object> variables,
			long taskInstanceId);

	public abstract Map<String, Object> populateVariables(long taskInstanceId);

	public abstract Map<String, Object> populateVariablesFromProcess(
			long processInstanceId);

}