package com.idega.jbpm.exe;

import com.idega.jbpm.view.View;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/30 11:12:24 $ by $Author: civilis $
 */
public interface TaskInstanceW {
	
	public abstract void submit(View view);
	
	public abstract void submit(View view, boolean proceedProcess);
	
	public abstract void start(int userId);
	
	public abstract void assign(int userId);
	
	public abstract void assign(User usr);
	
	public abstract User getAssignedTo();
	
	public abstract View loadView();
	
	public abstract Long getTaskInstanceId();
	
	public abstract void setTaskInstanceId(Long taskInstanceId);
}