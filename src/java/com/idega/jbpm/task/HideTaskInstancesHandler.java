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

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.transaction.TransactionContext;
import com.idega.transaction.TransactionalCallback;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/23 10:47:57 $ by $Author: civilis $
 */
@Service("hideTaskInstances")
@Scope("prototype")
public class HideTaskInstancesHandler implements ActionHandler {
	
	private static final long serialVersionUID = 5805122418836206146L;
	
	private String taskName;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	@Autowired
	private TransactionContext transactionContext;
	
	public void execute(final ExecutionContext ectx) throws Exception {
		
		getTransactionContext().executeInTransaction(
		    new TransactionalCallback() {
			    
			    public <T> T execute() {
				    
				    if (isValidParams()) {
					    
					    ProcessInstance mainProcessInstance = getBpmFactory()
					            .getMainProcessInstance(
					                ectx.getProcessInstance().getId());
					    
					    ProcessInstanceW mainPIW = getBpmFactory()
					            .getProcessInstanceW(
					                mainProcessInstance.getId());
					    
					    List<TaskInstanceW> tiws = mainPIW
					            .getUnfinishedTaskInstancesForTask(getTaskName());
					    
					    mainPIW.getTaskMgmtInstance().hideTaskInstances(tiws);
				    }
				    return null;
			    }
			    
		    });
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
	
	TransactionContext getTransactionContext() {
		return transactionContext;
	}
}