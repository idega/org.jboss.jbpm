package com.idega.jbpm.exe.impl;

import java.util.Map;

import com.idega.jbpm.business.ProcessManager;
import com.idega.jbpm.exe.Converter;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/10/01 16:32:27 $ by $Author: civilis $
 */
public class DefaultVariablesHandler implements VariablesHandler {

	private Converter converter;
	private ProcessManager processManager;

	public void submit(long tiId, Object submissionData) {

		if(getConverter() == null)
			throw new NullPointerException("Converter not set");

		Map<String, Object> variables = getConverter().convert(submissionData);
		
//		TODO: post to process ws here
		getProcessManager().submitVariables(variables, tiId);
	}
	
	public Object populate(long tiId, Object objectToPopulate) {
		
		if(getConverter() == null)
			throw new NullPointerException("Converter not set");
		
		Map<String, Object> variables = getProcessManager().populateVariables(tiId);
	
		return getConverter().revert(variables, objectToPopulate);
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