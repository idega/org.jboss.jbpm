package com.idega.jbpm.exe.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewFactory;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $
 *
 * Last modified: $Date: 2008/12/02 13:34:46 $ by $Author: civilis $
 */
@Scope("singleton")
@Service("bpmFactory")
public class BPMFactoryImpl implements BPMFactory {
	
	private final Map<String, String> creatorTypeCreatorBeanIdentifier;
	private final Map<String, String> viewTypeFactoryBeanIdentifier;
	
	private BPMDAO bindsDAO;
	private BPMContext idegaJbpmContext;
	private RolesManager rolesManager;
	private BPMUserFactory bpmUserFactory;
	
	public BPMFactoryImpl() {
		creatorTypeCreatorBeanIdentifier = new HashMap<String, String>(5);
		viewTypeFactoryBeanIdentifier = new HashMap<String, String>(5);
	}
	
	public ProcessManager getProcessManager(long processDefinitionId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessDefinition pd = ctx.getGraphSession().getProcessDefinition(processDefinitionId);
			return getManagersCreator(pd.getName()).getProcessManager();
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public View takeView(long taskInstanceId, boolean submitable, List<String> preferredTypes) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			TaskInstance ti = ctx.getTaskInstance(taskInstanceId);
			
			View view = getViewByTask(ti.getTask().getId(), submitable, preferredTypes);
			
			if(view != null) {

//				TODO: check, if view is taken for task instance id (just locate view by task instance)
				view.takeView();
				view.getViewToTask().bind(view, ti);
			}
			
			return view;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public View getViewByTask(long taskId, boolean submitable, List<String> preferredTypes) {
	
		List<ViewTaskBind> binds = getBindsDAO().getViewTaskBindsByTaskId(taskId);
		
		if(binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No view task bindings resolved for task. Task id: "+taskId);
			return null;
		}
		
		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds, preferredTypes);
		String viewType = viewTaskBind.getViewType();
		
		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
	}
	
	public View getViewByTaskInstance(long taskInstanceId, boolean submitable, List<String> preferredTypes) {
		
		List<ViewTaskBind> binds = getBindsDAO().getViewTaskBindsByTaskInstanceId(taskInstanceId);
		
		if(binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No view task bindings resolved for task intance. Task intance id: "+taskInstanceId);
			return null;
		}
		
		ViewTaskBind viewTaskBind = getPreferredViewTaskBind(binds, preferredTypes);
		String viewType = viewTaskBind.getViewType();
		
		ViewFactory viewFactory = getViewFactory(viewType);
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
	}
	
	protected ViewTaskBind getPreferredViewTaskBind(List<ViewTaskBind> binds, List<String> preferredTypes) {
		
		ViewTaskBind viewTaskBind = null;
		
		if(preferredTypes != null) {
			
			for (String preferredType : preferredTypes) {
				
				for (ViewTaskBind bind : binds) {
				
					if(preferredType.equals(bind.getViewType())) {
						viewTaskBind = bind;
						break;
					}
				}
			}
		}
		
		if(viewTaskBind == null && !binds.isEmpty())
			viewTaskBind = binds.get(0);
		
		return viewTaskBind;
	}
	
	protected ViewFactory getViewFactory(String viewType) {
		
		ViewFactory viewFactory;
		
		if(getViewTypeFactoryBeanIdentifier().containsKey(viewType)) {
			viewFactory = ELUtil.getInstance().getBean(getViewTypeFactoryBeanIdentifier().get(viewType));
			
		} else {
			throw new IllegalStateException("No View Factory registered for view type: "+viewType);
		}
		
		return viewFactory;
	}
	
	protected BPMManagersFactory getManagersCreator(String processName) {
		
		String managersType = resolveManagersType(processName);
		
		BPMManagersFactory creator;
		
		if(getCreatorTypeCreatorBeanIdentifier().containsKey(managersType)) {
			creator = ELUtil.getInstance().getBean(creatorTypeCreatorBeanIdentifier.get(managersType));
			
		} else {
			throw new IllegalStateException("No managers creator registered for type resolved: "+managersType+", process name: "+processName);
		}
		
		return creator;
	}
	
	protected String resolveManagersType(String processName) {

		ProcessManagerBind pm = getBindsDAO().getProcessManagerBind(processName);
		
		if(pm == null) {
			
			return "default";
		}

		return pm.getManagersType();
	}
	
	@Autowired(required=false)
	public void setBPManagersFactories(List<BPMManagersFactory> bpmManagersFactories) {
		
		for (BPMManagersFactory managersFactory : bpmManagersFactories) {
		
			if(managersFactory.getManagersType() == null)
				throw new IllegalArgumentException("Managers factory type not specified for factory: "+managersFactory);
			
			String beanIdentifier = managersFactory.getBeanIdentifier();
			
			if(beanIdentifier == null) {
				Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No bean identifier provided for managers factory, ignoring. Managers factory: "+managersFactory.getClass().getName());
			} else
				getCreatorTypeCreatorBeanIdentifier().put(managersFactory.getManagersType(), beanIdentifier);
		}
	}
	
	@Autowired(required=false)
	public void setViewsFactories(List<ViewFactory> viewsFactories) {
		
		for (ViewFactory viewFactory : viewsFactories) {
			
			if(viewFactory.getViewType() == null)
				throw new IllegalArgumentException("View factory type not specified for factory: "+viewFactory);
			
			String beanIdentifier = viewFactory.getBeanIdentifier();
			
			if(beanIdentifier == null) {
				Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No bean identifier provided for view factory, ignoring. View factory: "+viewFactory.getClass().getName());
			} else
				getViewTypeFactoryBeanIdentifier().put(viewFactory.getViewType(), beanIdentifier);
		}
	}

	protected Map<String, String> getCreatorTypeCreatorBeanIdentifier() {
		
		return creatorTypeCreatorBeanIdentifier;
	}

	public BPMDAO getBindsDAO() {
		return bindsDAO;
	}

	public Map<String, String> getViewTypeFactoryBeanIdentifier() {
		
		return viewTypeFactoryBeanIdentifier;
	}

	public ProcessManager getProcessManagerByTaskInstanceId(long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			
			TaskInstance taskInstance = ctx.getTaskInstance(taskInstanceId);
			long pdId = taskInstance.getProcessInstance().getProcessDefinition().getId();
			return getProcessManager(pdId);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}

	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setBindsDAO(BPMDAO bindsDAO) {
		this.bindsDAO = bindsDAO;
	}

	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}

	public BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	@Autowired
	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}

	public ProcessManager getProcessManagerByProcessInstanceId(long processInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			
			ProcessInstance processInstance = ctx.getProcessInstance(processInstanceId);
			long pdId = processInstance.getProcessDefinition().getId();
			return getProcessManager(pdId);
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	public ProcessManager getProcessManagerByType(String managerType) {
		
		BPMManagersFactory creator;
		
		if(getCreatorTypeCreatorBeanIdentifier().containsKey(managerType)) {
			creator = ELUtil.getInstance().getBean(creatorTypeCreatorBeanIdentifier.get(managerType));
			
		} else {
			throw new IllegalStateException("No managers creator registered for type resolved: "+managerType);
		}
		
		return creator.getProcessManager();
	}

	public ProcessManager getProcessManager(String processName) {

		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			ProcessDefinition pd = ctx.getGraphSession().findLatestProcessDefinition(processName);
			return getProcessManager(pd.getId());
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
}