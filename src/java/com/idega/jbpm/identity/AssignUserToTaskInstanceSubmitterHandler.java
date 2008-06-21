package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/21 16:47:05 $ by $Author: civilis $
 */
public class AssignUserToTaskInstanceSubmitterHandler implements ActionHandler {

	private static final long serialVersionUID = 340054091051722366L;
	private String taskInstanceIdExp;
	private String processInstanceIdExp;
	private String userIdExp;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		Long tid = getTaskInstanceIdExp() != null ? (Long)JbpmExpressionEvaluator.evaluate(getTaskInstanceIdExp(), ectx) : null;
		Integer userId = (Integer)JbpmExpressionEvaluator.evaluate(getUserIdExp(), ectx);
		
		if(userId != null) {
			
			TaskInstance ti = null;
			
			if(tid == null) {
			
				Long pid = (Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
				
				if(pid != null) {
					
//					no direct task in stance id set - resolving submitted start task instance from process instance
					
					ProcessInstance pi = ectx.getJbpmContext().getProcessInstance(pid);
					
					Task startTask = pi.getTaskMgmtInstance().getTaskMgmtDefinition().getStartTask();
					
					if(startTask != null) {
					
						@SuppressWarnings("unchecked")
						Collection<TaskInstance> tis = pi.getTaskMgmtInstance().getTaskInstances();
						
						for (TaskInstance taskInstance : tis) {
							
							if(taskInstance.getTask().equals(startTask)) {
								
								ti = taskInstance;
								break;
							}
						}
						
					} else {

						Logger.getLogger(getClass().getName()).log(Level.WARNING, "At AssignUserToTaskInstanceSubmitterHandler: No start task found for process instance (id="+pid+")");
					}
					
				} else {

					Logger.getLogger(getClass().getName()).log(Level.WARNING, "At AssignUserToTaskInstanceSubmitterHandler: No task instance id nor process instance id provided. Skipping submitter assignment.");
				}
			} else {
				
				ti = ectx.getJbpmContext().getTaskInstance(tid);
			}
			
			if(ti != null) {
				
				if(!ti.hasEnded()) {

					Logger.getLogger(getClass().getName()).log(Level.WARNING, "At AssignUserToTaskInstanceSubmitterHandler: Task instance (id="+tid+") not ended yet. Skipping submitter assignment. ");
				} else {
				
					String existingActorId = ti.getActorId();
					
					if(existingActorId != null) {
//						will override existing actor id
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "At AssignUserToTaskInstanceSubmitterHandler: Task instance (id="+tid+") resolved already has actor id (id="+existingActorId+") set, overriding with id="+userId);
					}
					
					ti.setActorId(userId.toString());
				}
			}
			
		} else {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "At AssignUserToTaskInstanceSubmitterHandler: No user id provided. Skipping submitter assignment.");
		}
	}

	public String getTaskInstanceIdExp() {
		return taskInstanceIdExp;
	}

	public void setTaskInstanceIdExp(String taskInstanceIdExp) {
		this.taskInstanceIdExp = taskInstanceIdExp;
	}

	public String getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(String processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}

	public String getUserIdExp() {
		return userIdExp;
	}

	public void setUserIdExp(String userIdExp) {
		this.userIdExp = userIdExp;
	}
}