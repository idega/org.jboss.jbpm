package com.idega.jbpm.exe;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/05 12:17:03 $ by $Author: civilis $
 */
public interface ProcessDefinitionW {
	
	public abstract void startProcess(View view);
	
	public abstract View loadInitView(int initiatorId);
	
	public abstract void setProcessDefinitionId(Long processDefinitionId);
	
	public abstract Long getProcessDefinitionId();
}