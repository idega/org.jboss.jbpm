package com.idega.jbpm.exe;

import java.util.List;

import com.idega.jbpm.view.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/09/02 12:59:58 $ by $Author: civilis $
 */
public interface ProcessDefinitionW {
	
	public abstract void startProcess(View view);
	
	public abstract View loadInitView(Integer initiatorId);
	
	public abstract void setProcessDefinitionId(Long processDefinitionId);
	
	public abstract Long getProcessDefinitionId();
	
	public abstract void setRolesCanStartProcess(List<String> roles, Object context);
	
	public abstract List<String> getRolesCanStartProcess(Object context);
}