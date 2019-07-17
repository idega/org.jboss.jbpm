package com.idega.jbpm.exe.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.data.SimpleQuerier;
import com.idega.hibernate.HibernateUtil;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessInstanceW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.PermissionsFactory;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewFactory;
import com.idega.jbpm.view.ViewSubmission;
import com.idega.jbpm.view.ViewSubmissionImpl;
import com.idega.presentation.IWContext;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreUtil;
import com.idega.util.DBUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.29 $ Last modified: $Date: 2009/03/20 19:18:18 $ by $Author: civilis $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service("bpmFactory")
public class BPMFactoryImpl implements BPMFactory {

	private static final Logger LOGGER = Logger.getLogger(BPMFactoryImpl.class.getName());

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

	private ProcessManager getJBPMProcessManager(final long processDefinitionId) {
		return getProcessManager(getBPMDAO().getProcessDefinitionNameByProcessDefinitionId(processDefinitionId));
	}

	@Override
	public <T extends Serializable> ProcessManager getProcessManager(T processDefinitionId) {
		//	BPM v. 1?
		if (processDefinitionId instanceof Number || (processDefinitionId != null && StringHandler.isNumeric(processDefinitionId.toString()))) {
			return getJBPMProcessManager(Long.valueOf(processDefinitionId.toString()));
		}

		//	Probably BPM v. 2
		Map<String, ProcessManager> managers = getAllProcessManagers();
		if (!MapUtil.isEmpty(managers)) {
			for (ProcessManager manager: managers.values()) {
				ProcessDefinitionW procDefW = null;
				try {
					procDefW = manager.getProcessDefinition(processDefinitionId);
				} catch (Exception e) {}
				if (procDefW != null) {
					return manager;
				}
			}
		}

		return null;
	}

	@Override
	@Transactional(readOnly = false)
	public View takeView(final long taskInstanceId, final boolean submitable, final List<String> preferredTypes) {
		try {
			return getBpmContext().execute(new JbpmCallback<View>() {

				@Override
				public View doInJbpm(JbpmContext context) throws JbpmException {
					TaskInstance ti = context.getTaskInstance(taskInstanceId);
					boolean initialize = false;
					if (ti == null) {
						ti = getBPMDAO().getSingleResultByInlineQuery(
								"from " + TaskInstance.class.getName() + " ti where ti.id = " + taskInstanceId,
								TaskInstance.class
						);
						initialize = ti != null;
					}
					if (ti == null) {
						LOGGER.warning("Unable to get task instance by ID: " + taskInstanceId);
						return null;
					}

					Task task = ti.getTask();
					if (task == null) {
						LOGGER.warning("Unable to get task from task instance with ID: " + taskInstanceId);
						return null;
					}

					if (initialize) {
						task = HibernateUtil.getInstance().initializeAndUnproxy(task);
					}

					View view = getViewByTask(task.getId(), submitable, preferredTypes);
					if (view != null) {
						takeView(view, ti);
					}

					return view;
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error taking view for task inst. with ID: " + taskInstanceId + ", submitable: " +
					submitable + ", types: " + preferredTypes, e);
		}
		return null;
	}

	protected void takeView(View view, TaskInstance ti) {
		if (!view.getViewToTask().containsBind(view.getViewType(), ti.getId())) {
			view.takeView();
			view.getViewToTask().bind(view, ti);
		} else {

		}
	}

	@Override
	public void takeViews(JbpmContext context, Task task, TaskInstance ti) {
		List<View> views = getViewsByTask(task.getId(), false);
		for (View view: views) {
			takeView(view, ti);
		}
	}

	@Override
	@Transactional(readOnly = false)
	public void takeViews(final long taskInstanceId) {
		getBpmContext().execute(new JbpmCallback<Void>() {

			@Override
			public Void doInJbpm(JbpmContext context) throws JbpmException {
				TaskInstance ti = context.getTaskInstance(taskInstanceId);
				List<View> views = getViewsByTask(ti.getTask().getId(), false);
				for (View view: views) {
					takeView(view, ti);
				}

				return null;
			}
		});
	}

	@Override
	@Transactional(readOnly = true)
	public View getViewByTask(long taskId, boolean submitable, List<String> preferredTypes) {
		List<ViewTaskBind> binds = getBPMDAO().getViewTaskBindsByTaskId(taskId);

		if (binds == null || binds.isEmpty()) {
			LOGGER.warning("No view task bindings resolved for task. Task id: " + taskId);
			return null;
		}

		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds, preferredTypes);
		String viewType = viewTaskBind.getViewType();

		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
	}

	@Transactional(readOnly = true)
	public List<View> getViewsByTask(long taskId, boolean submitable) {
		List<ViewTaskBind> binds = getBPMDAO().getViewTaskBindsByTaskId(taskId);

		if (ListUtil.isEmpty(binds)) {
			LOGGER.warning("No view task bindings resolved for task. Task id: " + taskId);
			return Collections.emptyList();
		}

		ArrayList<View> views = new ArrayList<View>(binds.size());

		for (ViewTaskBind viewTaskBind : binds) {
			String viewType = viewTaskBind.getViewType();

			ViewFactory viewFactory = getViewFactory(viewType);
			View view = viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);

			views.add(view);
		}

		return views;
	}

	@Override
	@Transactional(readOnly = true)
	public View getView(String viewIdentifier, String type, boolean submitable) {

		ViewFactory viewFactory = getViewFactory(type);
		return viewFactory.getView(viewIdentifier, submitable);
	}

	@Override
	@Transactional(readOnly = true)
	public View getViewByTaskInstance(long taskInstanceId, boolean submitable, List<String> preferredTypes, String... forcedTypes) {
		List<ViewTaskBind> binds = getBPMDAO().getViewTaskBindsByTaskInstanceId(taskInstanceId);

		if (ListUtil.isEmpty(binds)) {
			// no view taken - this is probably backward compatibility. In new manner, the view is
			// taken at TaskInstanceViewBindHandler, which is launched on task-create event of the
			// process (see configure method of ProcessBundleSingleProcessDefinitionImpl)

			takeViews(taskInstanceId);

			binds = getBPMDAO().getViewTaskBindsByTaskInstanceId(taskInstanceId);
		}

		if (ListUtil.isEmpty(binds)) {
			LOGGER.warning("No view task bindings resolved for task intance. Task intance id: " + taskInstanceId);
			return null;
		}

		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds, preferredTypes);
		String viewType = viewTaskBind.getViewType();

		ViewFactory viewFactory = ArrayUtil.isEmpty(forcedTypes) ? getViewFactory(viewType) : getViewFactory(forcedTypes[0]);
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
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

		if (viewTaskBind == null && !binds.isEmpty()) {
			viewTaskBind = binds.get(0);
		}

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

	@Override
	public BPMDAO getBPMDAO() {
		return bpmDAO;
	}

	@Override
	@Transactional(readOnly = true)
	public <T extends Serializable> ProcessManager getProcessManagerByTaskInstanceId(T taskInstId) {
		if (taskInstId instanceof Number || (taskInstId != null && StringHandler.isNumeric(taskInstId.toString()))) {
			final Long taskInstanceId = Long.valueOf(taskInstId.toString());

			return getBpmContext().execute(new JbpmCallback<ProcessManager>() {

				@Override
				public ProcessManager doInJbpm(JbpmContext context) throws JbpmException {
					TaskInstance taskInstance = context.getTaskInstance(taskInstanceId);
					return getProcessManager(taskInstance.getProcessInstance().getProcessDefinition().getName());
				}
			});
		}

		Map<String, ProcessManager> managers = getAllProcessManagers();
		if (!MapUtil.isEmpty(managers)) {
			for (ProcessManager manager: managers.values()) {
				TaskInstanceW taskInstW = null;
				ProcessDefinitionW procDefW = null;
				try {
					taskInstW = manager.getTaskInstance(taskInstId);
					if (taskInstW == null) {
						continue;
					}

					ProcessInstanceW piW = taskInstW.getProcessInstanceW();
					if (piW == null) {
						continue;
					}

					procDefW = piW.getProcessDefinitionW();
				} catch (Exception e) {}
				if (taskInstW != null && procDefW != null) {
					return manager;
				}
			}
		}

		throw new RuntimeException("Proc. manager can not be loaded by task instance ID: " + taskInstId);
	}

	@Override
	public <T extends Serializable> TaskInstanceW getTaskInstanceW(T taskInstanceId) {
		return getProcessManagerByTaskInstanceId(taskInstanceId).getTaskInstance(taskInstanceId);
	}

	@Override
	public <T extends Serializable> ProcessInstanceW getProcessInstanceW(T processInstanceId) {
		ProcessManager manager = getProcessManagerByProcessInstanceId(processInstanceId);
		if (manager != null) {
			return manager.getProcessInstance(processInstanceId);
		}

		return null;
	}

	@Override
	public <T extends Serializable> ProcessInstanceW getProcessInstanceW(JbpmContext context, T processInstanceId) {
		return getProcessManagerByProcessInstanceId(context, processInstanceId).getProcessInstance(processInstanceId);
	}

	@Override
	public ProcessDefinitionW getProcessDefinitionW(String processName) {
		return getProcessManager(processName).getProcessDefinition(processName);
	}

	public BPMContext getBpmContext() {
		return bpmContext;
	}

	@Override
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

	@Override
	public BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}

	@Override
	public <T extends Serializable> ProcessManager getProcessManagerByProcessInstanceId(final T processInstanceId) {
		return getBpmContext().execute(new JbpmCallback<ProcessManager>() {
			@Override
			public ProcessManager doInJbpm(JbpmContext context) throws JbpmException {
				return getProcessManagerByProcessInstanceId(context, processInstanceId);
			}
		});
	}

	private Map<String, ProcessManager> getAllProcessManagers() {
		ServletContext sc = null;
		IWContext iwc = CoreUtil.getIWContext();
		sc = iwc == null ? null : iwc.getServletContext();
		sc = sc == null ? IWMainApplication.getDefaultIWMainApplication().getServletContext() : sc;
		return WebApplicationContextUtils.getWebApplicationContext(sc).getBeansOfType(ProcessManager.class);
	}

	@Transactional(readOnly = true)
	private <T extends Serializable> ProcessManager getProcessManagerByProcessInstanceId(JbpmContext context, T proInstId) {
		if (proInstId instanceof Number || (proInstId != null && StringHandler.isNumeric(proInstId.toString()))) {
			Long processInstanceId = Long.valueOf(proInstId.toString());
			if (processInstanceId < 0) {
				LOGGER.warning("Invalid proc. inst. ID: " + processInstanceId);
			}

			ProcessInstance processInstance = context.getProcessInstance(processInstanceId);
			if (processInstance == null) {
				LOGGER.warning("Proc. inst. was not loaded from context by ID: " + processInstanceId + ", will try to load it from DB");
				processInstance = getBPMDAO().find(ProcessInstance.class, processInstanceId);
			}

			if (processInstance != null) {
				long pdId = processInstance.getProcessDefinition().getId();
				return getJBPMProcessManager(pdId);
			}
		}

		Map<String, ProcessManager> managers = getAllProcessManagers();
		if (!MapUtil.isEmpty(managers)) {
			for (ProcessManager manager: managers.values()) {
				ProcessInstanceW procInstW = null;
				ProcessDefinitionW procDefW = null;
				try {
					procInstW = manager.getProcessInstance(proInstId);
					if (procInstW == null) {
						continue;
					}

					procDefW = procInstW.getProcessDefinitionW();
				} catch (Exception e) {}
				if (procInstW != null && procDefW != null) {
					return manager;
				}
			}
		} else {
			LOGGER.warning("There are no process managers");
		}

		throw new RuntimeException("Proc. inst. can not be loaded by ID: " + proInstId + ". Tried with " + managers);
	}

	@Override
	public ProcessManager getProcessManagerByType(String managerType) {
		return getProcessManagers().get(managerType);
	}

	@Override
	public ProcessManager getProcessManager(String processName) {
		String managerType = resolveManagersType(processName);
		return getProcessManagerByType(managerType);
	}

	@Override
	public ViewSubmission getViewSubmission() {
		return new ViewSubmissionImpl();
	}

	@Override
	public PermissionsFactory getPermissionsFactory() {
		return permissionsFactory;
	}

	public Map<String, ProcessManager> getProcessManagers() {

		if (processManagers == null) {
			processManagers = Collections.emptyMap();
		}

		return processManagers;
	}

	public Map<String, ViewFactory> getViewFactories() {

		if (viewFactories == null) {
			viewFactories = Collections.emptyMap();
		}

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

	@Override
	public ProcessInstance getMainProcessInstance(JbpmContext context, final long processInstanceId) {
		ProcessInstance pi = context.getProcessInstance(processInstanceId);
		Long mainProcessInstanceId = (Long) pi.getContextInstance().getVariable(ProcessConstants.mainProcessInstanceIdVariableName);

		ProcessInstance mainProcessInstance;
		if (mainProcessInstanceId != null) {
			// if mainProcessInstanceId found in variable - use that
			mainProcessInstance = context.getProcessInstance(mainProcessInstanceId);
		} else {

			// if mainProcessInstanceId not found in variable - search
			// super processes. If we find mainProcessInstanceId in any
			// of the super process
			// we use that, else, use most super processInstance (one
			// without parent)

			Token superToken = pi.getSuperProcessToken();
			while (superToken != null && mainProcessInstanceId == null) {
				pi = superToken.getProcessInstance();
				mainProcessInstanceId = (Long) pi.getContextInstance().getVariable(ProcessConstants.mainProcessInstanceIdVariableName);

				if (mainProcessInstanceId == null) {
					superToken = pi.getSuperProcessToken();
				}
			}

			if (mainProcessInstanceId != null) {
				mainProcessInstance = context.getProcessInstance(mainProcessInstanceId);
			} else {
				mainProcessInstance = pi;
			}
		}

		return mainProcessInstance;
	}

	@Override
	public ProcessInstance getMainProcessInstance(final long processInstanceId) {
		return getBpmContext().execute(new JbpmCallback<ProcessInstance>() {
			@Override
			public ProcessInstance doInJbpm(JbpmContext context) throws JbpmException {
				return getMainProcessInstance(context, processInstanceId);
			}
		});
	}

	@Override
	public <T extends Serializable> T getIdOfStartTaskInstance(T piId, IWContext iwc) {
		if (piId instanceof Number || (piId != null && StringHandler.isNumeric(piId.toString()))) {
			@SuppressWarnings("unchecked")
			T id = (T) getIdOfStartTaskInstance(Long.valueOf(piId.toString()));
			return id;
		}

		ProcessInstanceW piW = getProcessInstanceW(piId);
		@SuppressWarnings("unchecked")
		T id  = (T) (piW == null ? null : piW.getIdOfStartTaskInstance(iwc));
		return id;
	}

	@Transactional(readOnly = true)
	private Long getIdOfStartTaskInstance(Long piId) {
		Long id = null;
		String query = "select t.id_ from jbpm_taskinstance t, jbpm_processinstance p, jbpm_processdefinition d, JBPM_MODULEDEFINITION m " +
				"where p.id_ = " + piId + " and t.PROCINST_ = p.id_ and p.processdefinition_ = d.id_ and " +
				"m.PROCESSDEFINITION_ = d.id_ and m.STARTTASK_ = t.TASK_";
		try {
			List<Serializable[]> result = SimpleQuerier.executeQuery(query, 1);
			if (!ListUtil.isEmpty(result)) {
				Serializable[] data = result.iterator().next();
				if (!ArrayUtil.isEmpty(data)) {
					Serializable startId = data[0];
					if (startId instanceof Number) {
						id = ((Number) startId).longValue();
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}

		if (id != null) {
			return id;
		}

		Long startTaskId = getBpmContext().execute(new JbpmCallback<Long>() {

			@Override
			public Long doInJbpm(JbpmContext context) throws JbpmException {
				ProcessInstance pi = context.getProcessInstance(piId);
				ProcessDefinition pd = DBUtil.getInstance().initializeAndUnproxy(pi.getProcessDefinition());
				TaskMgmtDefinition taskDef = DBUtil.getInstance().initializeAndUnproxy(pd.getTaskMgmtDefinition());
				Task startTask = DBUtil.getInstance().initializeAndUnproxy(taskDef.getStartTask());
				long startTaskId = startTask.getId();
				return startTaskId;
			}
		});
		return startTaskId;
	}

	@Override
	public <T extends Serializable> T getVariable(ExecutionContext ctx, String name) {
		if (ctx == null) {
			LOGGER.warning("Execution context is not provided");
			return null;
		}

		try {
			Long piId = ctx.getProcessInstance().getId();
			return getVariable(ctx, name, piId);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variable " + name + " from execution context " + ctx, e);
		}
		return null;
	}

	@Override
	public <T extends Serializable> T getVariable(ExecutionContext ctx, String name, Long piId) {
		if (StringUtil.isEmpty(name)) {
			LOGGER.warning("Variable's name is not provided!");
			return null;
		}

		if (ctx != null) {
			try {
				Object o = ctx.getVariable(name);
				if (o != null) {
					@SuppressWarnings("unchecked")
					T var = (T) o;
					return var;
				}
			} catch (Exception e) {}
		}

		if (piId != null) {
			List<Variable> vars = getBPMDAO().getVariablesByNameAndProcessInstance(name, piId);
			if (!ListUtil.isEmpty(vars)) {
				Variable var = vars.get(0);
				T value = var.getValue();
				return value;
			}
		}

		LOGGER.warning("Unable to get variable " + name + " for process instance " + piId);
		return null;
	}
}