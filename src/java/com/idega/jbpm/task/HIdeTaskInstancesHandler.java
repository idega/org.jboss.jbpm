package com.idega.jbpm.task;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/20 19:20:03 $ by $Author: civilis $
 */
@Service("hideTaskInstances")
@Scope("prototype")
public class HIdeTaskInstancesHandler implements ActionHandler {
	
	private static final long serialVersionUID = 5805122418836206146L;
	
	private String taskName;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		System.out.println("___HIDE by___TASK NAME=" + getTaskName());
		
		if (isValidParams()) {
			
			ProcessInstance mainProcessInstance = getBpmFactory()
			        .getMainProcessInstance(ectx.getProcessInstance().getId());
			
			ProcessInstanceW mainPIW = getBpmFactory().getProcessInstanceW(
			    mainProcessInstance.getId());
			
			List<TaskInstanceW> tiws = mainPIW
			        .getUnfinishedTaskInstancesForTask(getTaskName());
			
			mainPIW.getTaskMgmtInstance().hideTaskInstances(tiws);
		}
	}
	
	private boolean isValidParams() {
		
		if (StringUtil.isEmpty(getTaskName())) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING,
			    "Called remove task instances, but no taskName provided");
			return false;
		}
		
		return true;
	}
	
	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public String getTaskName() {
		return taskName;
	}
	
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}
}