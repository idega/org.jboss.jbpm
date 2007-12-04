package com.idega.jbpm.exe;

import javax.faces.context.FacesContext;

import org.jbpm.graph.exe.Token;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/12/04 18:49:48 $ by $Author: civilis $
 */
public interface ViewManager {

	public abstract View loadInitView(FacesContext context, Long processDefinitionId, int initiatorId);

//	TODO: if task instance finished - load form with submit buttons disabled and check if the task is not finished, when submitting variables
	public abstract View loadTaskInstanceView(FacesContext context, Long taskInstanceId);
	
	public abstract View loadProcessInstanceView(FacesContext context, Token token);
}