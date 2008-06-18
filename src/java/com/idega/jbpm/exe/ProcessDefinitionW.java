package com.idega.jbpm.exe;

import com.idega.jbpm.view.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/06/18 18:19:02 $ by $Author: civilis $
 */
public interface ProcessDefinitionW {
	
	public abstract void startProcess(View view);
	
	public abstract View loadInitView(Integer initiatorId);
	
	public abstract void setProcessDefinitionId(Long processDefinitionId);
	
	public abstract Long getProcessDefinitionId();
}