package com.idega.jbpm.exe;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.block.process.variables.Variable;
import com.idega.jbpm.view.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $
 * 
 *          Last modified: $Date: 2008/11/20 07:30:43 $ by $Author: valdas $
 */
public interface ProcessDefinitionW {

	public abstract void startProcess(View view);

	public abstract View loadInitView(Integer initiatorId);

	public abstract void setProcessDefinitionId(Long processDefinitionId);

	public abstract Long getProcessDefinitionId();

	public abstract ProcessDefinition getProcessDefinition();

	public abstract void setRolesCanStartProcess(List<String> roles,
			Object context);

	public abstract List<String> getRolesCanStartProcess(Object context);

	public abstract String getStartTaskName();

	public abstract List<Variable> getTaskVariableList(String taskName);

	public abstract Collection<String> getTaskNodeTransitionsNames(String taskName);
	
	public abstract String getProcessName(Locale locale);
}