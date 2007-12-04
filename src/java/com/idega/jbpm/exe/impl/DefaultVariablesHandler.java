package com.idega.jbpm.exe.impl;

import java.util.Map;

import com.idega.jbpm.exe.Converter;
import com.idega.jbpm.exe.VariablesHandler;
import com.idega.jbpm.exe.VariablesHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2007/12/04 14:06:02 $ by $Author: civilis $
 */
public class DefaultVariablesHandler {

	private Converter converter;
	private VariablesHandler processManager;

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

	public Object populateFromProcess(long processInstanceId, Object objectToPopulate) {
		
		if(getConverter() == null)
			throw new NullPointerException("Converter not set");
		
		Map<String, Object> variables = getProcessManager().populateVariablesFromProcess(processInstanceId);
	
		return getConverter().revert(variables, objectToPopulate);
	}
	
	public Converter getConverter() {
		return converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}

	public VariablesHandler getProcessManager() {
		return processManager;
	}

	public void setProcessManager(VariablesHandler processManager) {
		this.processManager = processManager;
	}
}