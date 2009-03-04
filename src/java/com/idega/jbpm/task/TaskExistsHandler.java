package com.idega.jbpm.task;

import java.util.Collection;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * checks, if non ended task instance exists in the process. Doesn't check the subprocesses
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/04 14:54:12 $ by $Author: civilis $
 */
@Service("taskExists")
@Scope("prototype")
public class TaskExistsHandler implements DecisionHandler {
	
	private static final long serialVersionUID = 5325029631660533882L;
	
	private String taskName;
	
	public String decide(ExecutionContext ectx) throws Exception {
		
		String taskName = getTaskName();
		
		if (StringUtil.isEmpty(taskName)) {
			throw new IllegalArgumentException("No task name provided");
		}
		
		@SuppressWarnings("unchecked")
		Collection<TaskInstance> taskInstances = ectx.getProcessInstance()
		        .getTaskMgmtInstance().getTaskInstances();
		
		if (!ListUtil.isEmpty(taskInstances)) {
			
			for (TaskInstance taskInstance : taskInstances) {
				
				if (!taskInstance.hasEnded()
				        && taskName.equals(taskInstance.getName())) {
					
					return "true";
				}
			}
		}
		
		return "false";
	}
	
	public String getTaskName() {
		return taskName;
	}
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	
}