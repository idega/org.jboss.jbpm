package com.idega.jbpm.exe.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
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
import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewFactory;
import com.idega.jbpm.view.ViewSubmissionImpl;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.22 $
 * 
 *          Last modified: $Date: 2008/12/28 12:08:04 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmFactory")
@Transactional(readOnly = true)
public class BPMFactoryImpl implements BPMFactory {

	private final Map<String, String> creatorTypeCreatorBeanIdentifier;
	private final Map<String, String> viewTypeFactoryBeanIdentifier;

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

	public BPMFactoryImpl() {
		creatorTypeCreatorBeanIdentifier = new HashMap<String, String>(5);
		viewTypeFactoryBeanIdentifier = new HashMap<String, String>(5);
	}

	public ProcessManager getProcessManager(final long processDefinitionId) {

		return getBpmContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				ProcessDefinition pd = context.getGraphSession()
						.getProcessDefinition(processDefinitionId);
				return getManagersCreator(pd.getName()).getProcessManager();
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

	public View getView(String viewIdentifier, String type, boolean submitable) {

		ViewFactory viewFactory = getViewFactory(type);
		return viewFactory.getView(viewIdentifier, submitable);
	}

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

	protected ViewFactory getViewFactory(String viewType) {

		ViewFactory viewFactory;

		if (getViewTypeFactoryBeanIdentifier().containsKey(viewType)) {
			viewFactory = ELUtil.getInstance().getBean(
					getViewTypeFactoryBeanIdentifier().get(viewType));

		} else {
			throw new IllegalStateException(
					"No View Factory registered for view type: " + viewType);
		}

		return viewFactory;
	}

	protected BPMManagersFactory getManagersCreator(String processName) {

		String managersType = resolveManagersType(processName);

		BPMManagersFactory creator;

		if (getCreatorTypeCreatorBeanIdentifier().containsKey(managersType)) {
			creator = ELUtil.getInstance().getBean(
					creatorTypeCreatorBeanIdentifier.get(managersType));

		} else {
			throw new IllegalStateException(
					"No managers creator registered for type resolved: "
							+ managersType + ", process name: " + processName);
		}

		return creator;
	}

	protected String resolveManagersType(String processName) {

		ProcessManagerBind pm = getBPMDAO().getProcessManagerBind(processName);

		if (pm == null) {

			return "default";
		}

		return pm.getManagersType();
	}

	@Autowired(required = false)
	public void setBPManagersFactories(
			List<BPMManagersFactory> bpmManagersFactories) {

		for (BPMManagersFactory managersFactory : bpmManagersFactories) {

			if (managersFactory.getManagersType() == null)
				throw new IllegalArgumentException(
						"Managers factory type not specified for factory: "
								+ managersFactory);

			String beanIdentifier = managersFactory.getBeanIdentifier();

			if (beanIdentifier == null) {
				Logger
						.getLogger(BPMFactory.class.getName())
						.log(
								Level.WARNING,
								"No bean identifier provided for managers factory, ignoring. Managers factory: "
										+ managersFactory.getClass().getName());
			} else
				getCreatorTypeCreatorBeanIdentifier().put(
						managersFactory.getManagersType(), beanIdentifier);
		}
	}

	@Autowired(required = false)
	public void setViewsFactories(List<ViewFactory> viewsFactories) {

		for (ViewFactory viewFactory : viewsFactories) {

			if (viewFactory.getViewType() == null)
				throw new IllegalArgumentException(
						"View factory type not specified for factory: "
								+ viewFactory);

			String beanIdentifier = viewFactory.getBeanIdentifier();

			if (beanIdentifier == null) {
				Logger.getLogger(BPMFactory.class.getName()).log(
						Level.WARNING,
						"No bean identifier provided for view factory, ignoring. View factory: "
								+ viewFactory.getClass().getName());
			} else
				getViewTypeFactoryBeanIdentifier().put(
						viewFactory.getViewType(), beanIdentifier);
		}
	}

	protected Map<String, String> getCreatorTypeCreatorBeanIdentifier() {

		return creatorTypeCreatorBeanIdentifier;
	}

	public BPMDAO getBPMDAO() {
		return bpmDAO;
	}

	public Map<String, String> getViewTypeFactoryBeanIdentifier() {

		return viewTypeFactoryBeanIdentifier;
	}

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

		BPMManagersFactory creator;

		if (getCreatorTypeCreatorBeanIdentifier().containsKey(managerType)) {
			creator = ELUtil.getInstance().getBean(
					creatorTypeCreatorBeanIdentifier.get(managerType));

		} else {
			throw new IllegalStateException(
					"No managers creator registered for type resolved: "
							+ managerType);
		}

		return creator.getProcessManager();
	}

	public ProcessManager getProcessManager(String processName) {

		return getManagersCreator(processName).getProcessManager();
	}

	public ViewSubmission getViewSubmission() {
		return new ViewSubmissionImpl();
	}

	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}
}