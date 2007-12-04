package com.idega.jbpm.exe;

import java.util.Map;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/12/04 14:06:02 $ by $Author: civilis $
 */
public interface Process {
	
	public abstract void startProcess(Map<String, String> parameters, Object submissionData);
	
	public abstract void proceedProcess(Map<String, String> parameters, Object submissionData);
	
	public abstract ViewManager getViewManager();
}