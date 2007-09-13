package com.idega.jbpm;

import java.util.Map;

import com.idega.jbpm.def.ViewToTask;
import com.idega.jbpm.exe.SubmissionHandler;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:23 $ by $Author: civilis $
 *
 */
public class ProcessManager {
	
	public ViewToTask getViewToTask(String identifier) {
		
		try {
			Class clazz = Class.forName("com.idega.formbuilder.business.process.XFormsToTask");
			
			return (ViewToTask)clazz.newInstance();
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<String, ViewToTask> getViewToTasks() {
		
		return null;
	}
	
	public SubmissionHandler getSubmissionHandler(String identifier) {
		
		return null;
	}
	
	public Map<String, SubmissionHandler> getSubmissionHandlers() {
		
		return null;
	}
}