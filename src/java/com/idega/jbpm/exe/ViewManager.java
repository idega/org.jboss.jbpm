package com.idega.jbpm.exe;

import javax.faces.context.FacesContext;

import org.jbpm.graph.exe.Token;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/12/04 14:06:02 $ by $Author: civilis $
 */
public interface ViewManager {

	public abstract View loadInitView(FacesContext context, Long processDefinitionId, int initiatorId);
	
	public abstract View loadTaskInstanceView(FacesContext context, Long taskInstanceId);
	
	public abstract View loadProcessInstanceView(FacesContext context, Token token);
}