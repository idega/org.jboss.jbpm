package com.idega.jbpm.exe.impl;

import java.util.List;
import java.util.Map;

import org.jbpm.context.def.VariableAccess;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.exe.Converter;
import com.idega.jbpm.exe.SubmissionHandler;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/21 11:29:39 $ by $Author: civilis $
 */
public class DefaultSubmissionHandler implements SubmissionHandler {
	
	private static final String identifier = "DEFAULT";
	private Converter converter;

	public void submit(TaskInstance ti, Object submissionData) {
		
		if(getConverter() == null)
			throw new NullPointerException("Converter not set");

		Map<String, Object> variables = getConverter().convert(submissionData);

		if(variables == null || variables.isEmpty())
			return;
		
		System.out.println("variables: "+variables);
		List<VariableAccess> variableAccesses = ti.getTask().getTaskController().getVariableAccesses();
		
		for (VariableAccess variableAccess : variableAccesses)
			if(!variableAccess.isWritable() && variables.containsKey(variableAccess.getVariableName()))
				variables.remove(variableAccess.getVariableName());
		
		ti.setVariables(variables);
		ti.getTask().getTaskController().submitParameters(ti);
		System.out.println("submitted...");
	}
	
	public Object populate(TaskInstance ti, Object objectToPopulate) {
		
		if(getConverter() == null)
			throw new NullPointerException("Converter not set");
	
		objectToPopulate = getConverter().revert(ti.getVariables(), objectToPopulate);
		
		System.out.println("returning populated...");
		
		return objectToPopulate;
	}
	
	public String getIdentifier() {
		
		return identifier;
	}

	public Converter getConverter() {
		return converter;
	}

	public void setConverter(Converter converter) {
		this.converter = converter;
	}
}