package com.idega.jbpm.exe;

import java.util.List;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.jbpm.view.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/09/11 11:14:12 $ by $Author: civilis $
 */
public interface ProcessDefinitionW {
	
	public abstract void startProcess(View view);
	
	public abstract View loadInitView(Integer initiatorId);
	
	public abstract void setProcessDefinitionId(Long processDefinitionId);
	
	public abstract Long getProcessDefinitionId();
	
	public abstract ProcessDefinition getProcessDefinition();
	
	public abstract void setRolesCanStartProcess(List<String> roles, Object context);
	
	public abstract List<String> getRolesCanStartProcess(Object context);
}