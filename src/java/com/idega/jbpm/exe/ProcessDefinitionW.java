package com.idega.jbpm.exe;

import com.idega.jbpm.view.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/05/19 13:52:40 $ by $Author: civilis $
 */
public interface ProcessDefinitionW {
	
	public abstract void startProcess(View view);
	
	public abstract View loadInitView(int initiatorId);
	
	public abstract void setProcessDefinitionId(Long processDefinitionId);
	
	public abstract Long getProcessDefinitionId();
}