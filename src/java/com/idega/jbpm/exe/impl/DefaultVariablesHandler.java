package com.idega.jbpm.exe.impl;

import java.util.Map;

import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.business.ProcessManager;
import com.idega.jbpm.exe.Converter;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/27 16:27:04 $ by $Author: civilis $
 */
public class DefaultVariablesHandler implements VariablesHandler {

	private Converter converter;
	private ProcessManager processManager;

	public void submit(long tiId, Object submissionData) {

		System.out.println("submiting... "+tiId);
		if(getConverter() == null)
			throw new NullPointerException("Converter not set");

		Map<String, Object> variables = getConverter().convert(submissionData);
		
//		TODO: post to process ws here
		getProcessManager().submitVariables(variables, tiId);
	}
	
	public Object populate(TaskInstance ti, Object objectToPopulate) {
		
		if(getConverter() == null)
			throw new NullPointerException("Converter not set");
	
		@SuppressWarnings("unchecked")
		Map<String, Object> variables = ti.getVariables();
		objectToPopulate = getConverter().revert(variables, objectToPopulate);
		
		System.out.println("returning populated...");
		
		return objectToPopulate;
	}
	
	public Converter getConverter() {
		return converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	public ProcessManager getProcessManager() {
		return processManager;
	}

	public void setProcessManager(ProcessManager processManager) {
		this.processManager = processManager;
	}
}