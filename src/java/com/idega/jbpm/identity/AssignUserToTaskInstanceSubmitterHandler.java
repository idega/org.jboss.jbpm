package com.idega.jbpm.identity;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 *          Last modified: $Date: 2008/11/30 08:17:27 $ by $Author: civilis $
 */
@Service("assignUserToTaskInstanceSubmitterHandler")
@Scope("prototype")
public class AssignUserToTaskInstanceSubmitterHandler implements ActionHandler {

	private static final long serialVersionUID = 340054091051722366L;
	private Long taskInstanceId;
	private Long processInstanceId;
	private Integer userId;

	public void execute(ExecutionContext ectx) throws Exception {

		Long tid = getTaskInstanceId();
		Integer userId = getUserId();

		if (userId != null) {

			TaskInstance ti = null;

			if (tid == null) {

				Long pid = getProcessInstanceId();

				if (pid != null) {

					// no direct task instance id set - resolving submitted
					// start task instance from process instance

					ProcessInstance pi = ectx.getJbpmContext()
							.getProcessInstance(pid);

					Task startTask = pi.getTaskMgmtInstance()
							.getTaskMgmtDefinition().getStartTask();

					if (startTask != null) {

						@SuppressWarnings("unchecked")
						Collection<TaskInstance> tis = pi.getTaskMgmtInstance()
								.getTaskInstances();

						for (TaskInstance taskInstance : tis) {

							if (taskInstance.getTask().equals(startTask)) {

								ti = taskInstance;
								break;
							}
						}

					} else {

						Logger
								.getLogger(getClass().getName())
								.log(
										Level.WARNING,
										"At AssignUserToTaskInstanceSubmitterHandler: No start task found for process instance (id="
												+ pid + ")");
					}

				} else {

					Logger
							.getLogger(getClass().getName())
							.log(
									Level.WARNING,
									"At AssignUserToTaskInstanceSubmitterHandler: No task instance id nor process instance id provided. Skipping submitter assignment.");
				}
			} else {

				ti = ectx.getJbpmContext().getTaskInstance(tid);
			}

			if (ti != null) {

				if (!ti.hasEnded()) {

					Logger
							.getLogger(getClass().getName())
							.log(
									Level.WARNING,
									"At AssignUserToTaskInstanceSubmitterHandler: Task instance (id="
											+ tid
											+ ") not ended yet. Skipping submitter assignment. ");
				} else {

					String existingActorId = ti.getActorId();

					if (existingActorId != null) {
						// will override existing actor id
						Logger
								.getLogger(getClass().getName())
								.log(
										Level.WARNING,
										"At AssignUserToTaskInstanceSubmitterHandler: Task instance (id="
												+ tid
												+ ") resolved already has actor id (id="
												+ existingActorId
												+ ") set, overriding with id="
												+ userId);
					}

					ti.setActorId(userId.toString());
				}
			}

		} else {
			Logger
					.getLogger(getClass().getName())
					.log(
							Level.WARNING,
							"At AssignUserToTaskInstanceSubmitterHandler: No user id provided. Skipping submitter assignment. Called from process instance id = "
									+ ectx.getProcessInstance().getId());
		}
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}
}