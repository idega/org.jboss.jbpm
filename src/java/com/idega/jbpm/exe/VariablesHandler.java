package com.idega.jbpm.exe;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/27 16:26:43 $ by $Author: civilis $
 */
public interface VariablesHandler {

	public abstract void submit(long tiId, Object submissionData);
}