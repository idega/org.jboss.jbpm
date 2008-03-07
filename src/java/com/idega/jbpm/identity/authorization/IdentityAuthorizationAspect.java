package com.idega.jbpm.identity.authorization;

import java.security.Permission;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jbpm.JbpmContext;
import org.jbpm.security.AuthorizationService;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.business.BPMPointcuts;
import com.idega.jbpm.data.dao.BpmBindsDAO;
import com.idega.jbpm.identity.permission.SubmitTaskParametersPermission;
import com.idega.jbpm.identity.permission.ViewTaskParametersPermission;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/03/07 13:26:41 $ by $Author: civilis $
 */
@Aspect
public class IdentityAuthorizationAspect {

	private IdegaJbpmContext idegaJbpmContext;
	private BpmBindsDAO bpmBindsDAO;
	private AuthorizationService authorizationService;

	/*
	@Before(BPMPointcuts.startProcessAtProcessManager+" && args(processDefinitionId, ..)")
	public void checkPermissionToStartProcess(JoinPoint p, long processDefinitionId) {

//		TODO: permitting everyone to start process
	}
	*/
	
	@Before("("+BPMPointcuts.loadTaskInstanceViewAtViewManager+" || "+BPMPointcuts.submitProcessAtProcessManager+") && args(taskInstanceId, ..)")
	public void checkPermissionToTaskInstance(long taskInstanceId) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

		try {
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			Permission permission;
			
			if(taskInstance.hasEnded()) {
				
				if(taskInstance.getActorId() != null) {
					
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Task instance has aneded, but it is still assigned to actorId, fixing this now. Task instance id: "+taskInstance.getId());
					taskInstance.setActorId(null);
				}
				
				permission = new ViewTaskParametersPermission("taskInstance", null, taskInstance);
				
			} else {
				
				permission = new SubmitTaskParametersPermission("taskInstance", null, taskInstance);
			}
			
			getAuthorizationService().checkPermission(permission);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public BpmBindsDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}

	public void setBpmBindsDAO(BpmBindsDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}

	public AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	public void setAuthorizationService(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}
}