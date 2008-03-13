package com.idega.jbpm.exe.impl;

import java.util.Date;

import org.jbpm.JbpmContext;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.def.View;
import com.idega.jbpm.exe.BPMAccessControlException;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessException;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.identity.RolesManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/13 21:05:45 $ by $Author: civilis $
 */
public abstract class AbstractProcessManager implements ProcessManager {
	
	public abstract void startProcess(long processDefinitionId, View view);
	
	public abstract void submitTaskInstance(long taskInstanceId, View view);
	
	public abstract IdegaJbpmContext getIdegaJbpmContext();
	
	public abstract AuthorizationService getAuthorizationService();

	public abstract BPMFactory getBpmFactory();

	public void startTask(long taskInstanceId, int userId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			rolesManager.hasRightsToStartTask(taskInstanceId, userId);
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			taskInstance.setStart(new Date());
			ctx.save(taskInstance);
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public void assignTask(long taskInstanceId, int assignToUserId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			RolesManager rolesManager = getBpmFactory().getRolesManager();
			rolesManager.hasRightsToAsssignTask(taskInstanceId, assignToUserId);
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			taskInstance.setActorId(String.valueOf(assignToUserId));
			ctx.save(taskInstance);
		
		} catch (BPMAccessControlException e) {
			throw new ProcessException(e, e.getUserFriendlyMessage());
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
}