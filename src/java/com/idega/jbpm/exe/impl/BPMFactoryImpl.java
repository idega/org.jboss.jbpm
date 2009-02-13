package com.idega.jbpm.exe.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewFactory;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.jbpm.view.ViewSubmissionImpl;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.26 $
 * 
 *          Last modified: $Date: 2009/02/13 17:27:48 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmFactory")
public class BPMFactoryImpl implements BPMFactory {

	private Map<String, ProcessManager> processManagers;
	private Map<String, ViewFactory> viewFactories;

	@Autowired
	private BPMDAO bpmDAO;
	@Autowired
	private BPMContext bpmContext;
	@Autowired
	private RolesManager rolesManager;
	@Autowired
	private BPMUserFactory bpmUserFactory;
	@Autowired
	private PermissionsFactory permissionsFactory;

	@Transactional(readOnly = true)
	public ProcessManager getProcessManager(final long processDefinitionId) {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				ProcessDefinition pd = context.getGraphSession()
						.getProcessDefinition(processDefinitionId);
				return getProcessManager(pd.getName());
			}
		});
	}

	@Transactional(readOnly = false)
	public View takeView(final long taskInstanceId, final boolean submitable,
			final List<String> preferredTypes) {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				TaskInstance ti = context.getTaskInstance(taskInstanceId);

				View view = getViewByTask(ti.getTask().getId(), submitable,
						preferredTypes);

				if (view != null) {

					// TODO: check, if view is taken for task instance id (just
					// locate view by task instance)
					view.takeView();
					view.getViewToTask().bind(view, ti);
				}

				return view;
			}
		});
	}

	@Transactional(readOnly = true)
	public View getViewByTask(long taskId, boolean submitable,
			List<String> preferredTypes) {

		List<ViewTaskBind> binds = getBPMDAO().getViewTaskBindsByTaskId(taskId);

		if (binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(
					Level.WARNING,
					"No view task bindings resolved for task. Task id: "
							+ taskId);
			return null;
		}

		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds,
				preferredTypes);
		String viewType = viewTaskBind.getViewType();

		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory
				.getView(viewTaskBind.getViewIdentifier(), submitable);
	}

	@Transactional(readOnly = true)
	public View getView(String viewIdentifier, String type, boolean submitable) {

		ViewFactory viewFactory = getViewFactory(type);
		return viewFactory.getView(viewIdentifier, submitable);
	}

	@Transactional(readOnly = true)
	public View getViewByTaskInstance(long taskInstanceId, boolean submitable,
			List<String> preferredTypes) {

		List<ViewTaskBind> binds = getBPMDAO()
				.getViewTaskBindsByTaskInstanceId(taskInstanceId);

		if (binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(
					Level.WARNING,
					"No view task bindings resolved for task intance. Task intance id: "
							+ taskInstanceId);
			return null;
		}

		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds,
				preferredTypes);
		String viewType = viewTaskBind.getViewType();

		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory
				.getView(viewTaskBind.getViewIdentifier(), submitable);
	}

	@Transactional(readOnly = true)
	protected ViewTaskBind getPreferredViewTaskBind(List<ViewTaskBind> binds,
			List<String> preferredTypes) {

		ViewTaskBind viewTaskBind = null;

		if (preferredTypes != null) {

			for (String preferredType : preferredTypes) {

				for (ViewTaskBind bind : binds) {

					if (preferredType.equals(bind.getViewType())) {
						viewTaskBind = bind;
						break;
					}
				}
			}
		}

		if (viewTaskBind == null && !binds.isEmpty())
			viewTaskBind = binds.get(0);

		return viewTaskBind;
	}

	@Transactional(readOnly = true)
	protected ViewFactory getViewFactory(String viewType) {

		ViewFactory viewFactory;

		if (getViewFactories().containsKey(viewType)) {
			viewFactory = getViewFactories().get(viewType);

		} else {
			throw new IllegalStateException(
					"No View Factory registered for view type: " + viewType);
		}

		return viewFactory;
	}

	@Transactional(readOnly = true)
	protected String resolveManagersType(String processName) {

		ProcessManagerBind pm = getBPMDAO().getProcessManagerBind(processName);

		if (pm == null) {

			return "default";
		}

		return pm.getManagersType();
	}

	@Autowired(required = false)
	public void setProcessManagers(List<ProcessManager> processManagersList) {

		// hashmap is thread safe for read only ops
		processManagers = new HashMap<String, ProcessManager>(
				processManagersList.size());

		for (ProcessManager processManager : processManagersList) {

			processManagers
					.put(processManager.getManagerType(), processManager);
		}
	}

	public BPMDAO getBPMDAO() {
		return bpmDAO;
	}

	@Transactional(readOnly = true)
	public ProcessManager getProcessManagerByTaskInstanceId(
			final long taskInstanceId) {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance taskInstance = context
						.getTaskInstance(taskInstanceId);
				long pdId = taskInstance.getProcessInstance()
						.getProcessDefinition().getId();
				return getProcessManager(pdId);
			}
		});
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	public void setBPMDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}

	public void setBpmContext(BPMContext bpmContext) {
		this.bpmContext = bpmContext;
	}

	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}

	@Transactional(readOnly = true)
	public ProcessManager getProcessManagerByProcessInstanceId(
			final long processInstanceId) {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance processInstance = context
						.getProcessInstance(processInstanceId);
				long pdId = processInstance.getProcessDefinition().getId();
				return getProcessManager(pdId);
			}
		});
	}

	public ProcessManager getProcessManagerByType(String managerType) {

		return getProcessManagers().get(managerType);
	}

	public ProcessManager getProcessManager(String processName) {

		String managerType = resolveManagersType(processName);
		return getProcessManagerByType(managerType);
	}

	public ViewSubmission getViewSubmission() {
		return new ViewSubmissionImpl();
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public Map<String, ProcessManager> getProcessManagers() {

		if (processManagers == null)
			processManagers = Collections.emptyMap();

		return processManagers;
	}

	public Map<String, ViewFactory> getViewFactories() {

		if (viewFactories == null)
			viewFactories = Collections.emptyMap();

		return viewFactories;
	}

	@Autowired(required = false)
	public void setViewFactories(List<ViewFactory> viewFactoriesList) {

		// hashmap is thread safe for read only ops
		viewFactories = new HashMap<String, ViewFactory>(viewFactoriesList
				.size());

		for (ViewFactory viewFactory : viewFactoriesList) {

			viewFactories.put(viewFactory.getViewType(), viewFactory);
		}
	}

	public ProcessInstance getMainProcessInstance(final long processInstanceId) {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				ProcessInstance pi = context
						.getProcessInstance(processInstanceId);
				Long mainProcessInstanceId = (Long) pi
						.getContextInstance()
						.getVariable(
								ProcessConstants.mainProcessInstanceIdVariableName);

				ProcessInstance mainProcessInstance;

				if (mainProcessInstanceId != null) {

					// if mainProcessInstanceId found in variable - use that

					mainProcessInstance = context
							.getProcessInstance(mainProcessInstanceId);

				} else {

					// if mainProcessInstanceId not found in variable - search
					// super processes. If we find mainProcessInstanceId in any
					// of the super process
					// we use that, else, use most super processInstance (one
					// without parent)

					Token superToken = pi.getSuperProcessToken();

					while (superToken != null && mainProcessInstanceId == null) {

						pi = superToken.getProcessInstance();

						mainProcessInstanceId = (Long) pi
								.getContextInstance()
								.getVariable(
										ProcessConstants.mainProcessInstanceIdVariableName);

						if (mainProcessInstanceId == null) {

							superToken = pi.getSuperProcessToken();
						}
					}

					if (mainProcessInstanceId != null) {

						mainProcessInstance = context
								.getProcessInstance(mainProcessInstanceId);
					} else
						mainProcessInstance = pi;
				}

				return mainProcessInstance;
			}
		});
	}
}