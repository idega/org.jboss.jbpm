package com.idega.jbpm.exe;

import javax.faces.context.FacesContext;
import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/01/25 15:24:25 $ by $Author: civilis $
 */
public interface ViewManager {

	public abstract View loadInitView(FacesContext context, Long processDefinitionId, int initiatorId);

	public abstract View loadTaskInstanceView(FacesContext context, Long taskInstanceId);
}