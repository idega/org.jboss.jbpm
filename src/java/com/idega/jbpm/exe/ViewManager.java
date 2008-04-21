package com.idega.jbpm.exe;

import com.idega.jbpm.def.View;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/04/21 05:13:44 $ by $Author: civilis $
 */
public interface ViewManager {

	public abstract View loadInitView(long processDefinitionId, int initiatorId);

	public abstract View loadTaskInstanceView(long taskInstanceId);
}