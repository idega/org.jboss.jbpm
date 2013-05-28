package com.idega.jbpm.task;

import java.util.List;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.hibernate.HibernateUtil;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
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

	@Override
	public TaskMgmtInstanceW init(ProcessInstanceW piw) {

		if (this.piw == null)
			this.piw = piw;

		return this;
	}

	@Override
	@Transactional(readOnly = false)
	public TaskInstanceW createTask(final String taskName, final long tokenId, final boolean loadView) {
		TaskInstanceW tiW = getBpmContext().execute(new JbpmCallback<TaskInstanceW>() {

			@Override
			public TaskInstanceW doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance processInstance = getPiw().getProcessInstance(context);

				TokenTaskNode tokenTaskNode = getTaskNode(context, tokenId);
				if (tokenTaskNode == null)
					throw new RuntimeException("Token by ID " + tokenId + " can not be found for proc. inst. " + processInstance +
							". Failed to create task '" + taskName + "'");

				Token token = tokenTaskNode.token;
				TaskNode taskNode = tokenTaskNode.taskNode;

				Task task = taskNode.getTask(taskName);
				if (task == null) {
					Long pdId = processInstance.getProcessDefinition().getId();
					task = getBpmFactory().getBPMDAO().getSingleResultByInlineQuery("from " + Task.class.getName() +
							" t where t.name = :name and t.processDefinition.id = :pdId", Task.class,
							new Param("name", taskName),
							new Param("pdId", pdId)
					);
				}

				TaskInstance ti = processInstance.getTaskMgmtInstance().createTaskInstance(task, token);
				Long tiId = ti.getId();

				ProcessManager pm = getBpmFactory().getProcessManager(processInstance.getProcessDefinition().getName());
				TaskInstanceW taskInstanceW = pm.getTaskInstance(tiId);
				if (loadView)
					taskInstanceW.loadView();
				return taskInstanceW;
			}
		});

		if (tiW == null)
			throw new RuntimeException("Error creating task '" + taskName + "' for token with ID: " + tokenId);

		return tiW;
	}

	private class TokenTaskNode {
		private Token token;
		private TaskNode taskNode;

		private TokenTaskNode(Token token, TaskNode taskNode) {
			this.token = token;
			this.taskNode = taskNode;
		}
	}

	private TokenTaskNode getTaskNode(JbpmContext context, Long tokenId) {
		if (context == null || tokenId == null) {
			return null;
		}

		Token token = context.getToken(tokenId);
		if (token == null) {
			return null;
		}

		Node node = token.getNode();
		node = HibernateUtil.initializeAndUnproxy(node);
		if (!(node instanceof TaskNode)) {
			Token parent = token.getParent();
			parent = HibernateUtil.initializeAndUnproxy(parent);
			return getTaskNode(context, parent.getId());
		}

		return new TokenTaskNode(token, (TaskNode) node);
	}

	@Override
	@Transactional(readOnly = false)
	public void hideTaskInstances(List<TaskInstanceW> tiws) {
		for (TaskInstanceW tiw: tiws) {
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