package com.idega.jbpm.exe.impl;

import java.security.AccessControlException;
import java.util.Date;

import org.jbpm.JbpmContext;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.def.View;
import com.idega.jbpm.exe.ProcessException;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/13 17:00:38 $ by $Author: civilis $
 */
public abstract class AbstractProcessManager implements ProcessManager {
	
	public abstract void startProcess(long processDefinitionId, View view);
	
	public abstract void submitTaskInstance(long taskInstanceId, View view);
	
	public abstract IdegaJbpmContext getIdegaJbpmContext();
	
	public abstract void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext);
	
	public abstract AuthorizationService getAuthorizationService();

	public abstract void setAuthorizationService(AuthorizationService authorizationService);
	
	public void startTask(long taskInstanceId, int userId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.getStart() != null || taskInstance.hasEnded())
				throw new ProcessException("Task ("+taskInstanceId+") has already been started, or has already ended", "Task has already been started, or has already ended");
			
			if(taskInstance.getActorId() == null || !taskInstance.getActorId().equals(userId))
				throw new ProcessException("User ("+userId+") tried to start task, but not assigned to the user provided. Assigned: "+taskInstance.getActorId(), "User should be taken or assigned of the task first to start working on it");
			
			SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			getAuthorizationService().checkPermission(permission);
			
			taskInstance.setStart(new Date());
			ctx.save(taskInstance);
		
		} catch (AccessControlException e) {
			throw new ProcessException(e, "User has no access to modify this task");
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public void assignTask(long taskInstanceId, int assignToUserId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			
			if(taskInstance.getStart() != null || taskInstance.hasEnded())
				throw new ProcessException("Task ("+taskInstanceId+") has already been started, or has already ended", "Task has been started by someone, or has ended.");
			
			if(taskInstance.getActorId() != null)
				throw new ProcessException("Tried to assign (to "+assignToUserId+") already assigned task ("+taskInstanceId+"). ", "This task has been assigned already");
			
			SubmitTaskParametersPermission permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			getAuthorizationService().checkPermission(permission);
			
			taskInstance.setActorId(String.valueOf(assignToUserId));
			ctx.save(taskInstance);
		
		} catch (AccessControlException e) {
			throw new ProcessException(e, "User has no access to modify this task");
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
}