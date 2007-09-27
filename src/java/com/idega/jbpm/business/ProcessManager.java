package com.idega.jbpm.business;

import java.util.List;
import java.util.Map;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/27 16:26:43 $ by $Author: civilis $
 */
public class ProcessManager {

	private JbpmConfiguration jbpmConfiguration;
	
	public void submitVariables(Map<String, Object> variables, long taskInstanceId) {

		System.out.println("submitting variables: "+variables+" to: "+taskInstanceId);
		JbpmContext ctx = getJbpmConfiguration().createJbpmContext();
		
		try {
			if(variables == null || variables.isEmpty())
				return;

			if(true)
				return;
			
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = ti.getTask().getTaskController().getVariableAccesses();
			
			for (VariableAccess variableAccess : variableAccesses)
				if(!variableAccess.isWritable() && variables.containsKey(variableAccess.getVariableName()))
					variables.remove(variableAccess.getVariableName());

			ti.setVariables(variables);
			ti.getTask().getTaskController().submitParameters(ti);
			
		} finally {
			ctx.close();
		}
	}

	public JbpmConfiguration getJbpmConfiguration() {
		return jbpmConfiguration;
	}

	public void setJbpmConfiguration(JbpmConfiguration jbpmConfiguration) {
		this.jbpmConfiguration = jbpmConfiguration;
	}
}