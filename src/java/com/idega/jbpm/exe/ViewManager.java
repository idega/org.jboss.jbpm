package com.idega.jbpm.exe;

import javax.faces.context.FacesContext;
import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/03/06 11:55:03 $ by $Author: civilis $
 */
public interface ViewManager {

	public abstract View loadInitView(long processDefinitionId, int initiatorId, FacesContext context);

	public abstract View loadTaskInstanceView(long taskInstanceId, FacesContext context);
}