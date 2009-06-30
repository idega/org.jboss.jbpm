package com.idega.jbpm.task;

import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.exe.TaskMgmtInstanceW;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $ Last modified: $Date: 2009/06/30 13:15:55 $ by $Author: valdas $
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Service
@Qualifier("default")
public class TaskMgmtInstanceDefaultW implements TaskMgmtInstanceW {
	
	private ProcessInstanceW piw;
	
	@Autowired
	private BPMContext bpmContext;
	
	@Autowired
	private BPMFactory bpmFactory;
	
	public TaskMgmtInstanceW init(ProcessInstanceW piw) {
		
		if (this.piw == null)
			this.piw = piw;
		
		return this;
	}
	
	@Transactional(readOnly = false)
	public TaskInstanceW createTask(final String taskName, final long tokenId) {
		
		return getBpmContext().execute(new JbpmCallback() {
			
			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance processInstance = getPiw().getProcessInstance();
				
				@SuppressWarnings("unchecked")
				List<Token> tkns = processInstance.findAllTokens();
				
				for (Token token : tkns) {
					
					if (token.getId() == tokenId) {
						TaskInstance ti = processInstance.getTaskMgmtInstance()
						        .createTaskInstance(
						            ((TaskNode) token.getNode())
						                    .getTask(taskName), token);
						/*
						 * getBpmFactory().takeView(ti.getId(), true,
						 * preferred);
						 */

						TaskInstanceW taskInstanceW = getBpmFactory()
						        .getProcessManagerByTaskInstanceId(ti.getId())
						        .getTaskInstance(ti.getId());
						
						taskInstanceW.loadView();
						return taskInstanceW;
						
					}
				}
				
				return null;
			}
		});
	}
	
	@Transactional(readOnly = false)
	public void hideTaskInstances(List<TaskInstanceW> tiws) {
		
		for (TaskInstanceW tiw : tiws) {
			
			tiw.hide();
		}
	}
	
	protected ProcessInstanceW getPiw() {
		return piw;
	}
	
	BPMContext getBpmContext() {
		return bpmContext;
	}
	
	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
}