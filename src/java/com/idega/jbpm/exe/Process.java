package com.idega.jbpm.exe;

import java.util.Map;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/12/05 10:36:31 $ by $Author: civilis $
 */
public interface Process {
	
	public abstract void startProcess(Map<String, String> parameters, Object submissionData);
	
	public abstract void submitTaskInstance(Map<String, String> parameters, Object submissionData);
	
	public abstract ViewManager getViewManager();
}