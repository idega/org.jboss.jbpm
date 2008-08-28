package com.idega.jbpm.exe;

import java.util.Locale;

import com.idega.jbpm.identity.Role;
import com.idega.jbpm.view.View;
import com.idega.user.data.User;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/08/28 12:10:02 $ by $Author: civilis $
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
	
	public abstract String getName(Locale locale);
	
	public abstract TaskInstance getTaskInstance();
	
	public abstract void setTaskRolePermissions(Role role, boolean setSameForAttachments, String variableIdentifier);
}