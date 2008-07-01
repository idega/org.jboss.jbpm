package com.idega.jbpm.invitation;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/07/01 19:38:40 $ by $Author: civilis $
 */
public class AssignTasksForRolesUsers implements ActionHandler {

	private static final long serialVersionUID = -1873068611362644702L;
	private String tasksExp;

	public void execute(ExecutionContext ectx) throws Exception {

		@SuppressWarnings("unchecked")
		final List<AssignTasksForRolesUsersBean> tasksBeans = (List<AssignTasksForRolesUsersBean>)JbpmExpressionEvaluator.evaluate(getTasksExp(), ectx);
		
		System.out.println("SUBPROCESS ID="+ectx.getProcessInstance().getId());

		for (AssignTasksForRolesUsersBean tb : tasksBeans) {

			Task task = tb.getTask();
			System.out.println("__roles="+tb.getRoles());
			System.out.println("______task="+task);
			
			///TaskNode taskNode = (TaskNode)ectx.getProcessDefinition().getNode("tasksForAllRolesUsers");
			try {
				TaskInstance ti = ectx.getTaskMgmtInstance().createTaskInstance(task, tb.getToken());
				System.out.println("______created ti, assigning="+ti);
				ti.setActorId("xx");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String getTasksExp() {
		return tasksExp;
	}

	public void setTasksExp(String tasksExp) {
		this.tasksExp = tasksExp;
	}
}