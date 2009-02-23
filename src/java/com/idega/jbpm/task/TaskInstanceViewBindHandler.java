package com.idega.jbpm.task;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/02/23 12:36:15 $ by $Author: civilis $
 */
@Service(TaskInstanceViewBindHandler.beanIdentifier)
@Scope("prototype")
public class TaskInstanceViewBindHandler implements ActionHandler {

	public static final String beanIdentifier = "taskInstanceViewBindHandler";

	private static final long serialVersionUID = 4836485476800659735L;

	@Autowired
	private BPMFactory bpmFactory;

	public void execute(ExecutionContext ectx) throws Exception {

		TaskInstance ti = ectx.getTaskInstance();

		Task startTask = ti.getTaskMgmtInstance().getTaskMgmtDefinition()
				.getStartTask();

		if (!ti.getTask().equals(startTask)) {

			// we are not binding start task instance, because it is already
			// bound in the loadInitView of processDefinitionW

			getBpmFactory().takeViews(ti.getId());
		}
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
}