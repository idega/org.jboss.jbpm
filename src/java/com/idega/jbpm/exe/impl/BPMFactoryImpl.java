package com.idega.jbpm.exe.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.def.View;
import com.idega.jbpm.def.ViewFactory;
import com.idega.jbpm.def.impl.ViewFactoryPluggedInEvent;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.BPMManagersFactory;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.ViewManager;
import com.idega.jbpm.identity.RolesManager;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/03/13 21:05:45 $ by $Author: civilis $
 */
public class BPMFactoryImpl implements BPMFactory, ApplicationListener, ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private Map<String, String> creatorTypeCreatorBeanIdentifier;
	private Map<String, Object> creatorTypeCreatorClass;
	private Map<String, String> viewTypeFactoryBeanIdentifier;
	private Map<String, Object> viewTypeFactoryClass;
	
	private BPMDAO bindsDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private RolesManager rolesManager;
	
	

	public ProcessManager getProcessManager(long processDefinitionId) {

		return getManagersCreator(processDefinitionId).getProcessManager();
	}

	public ViewManager getViewManager(long processDefinitionId) {
		
		return getManagersCreator(processDefinitionId).getViewManager();
	}
	
	public View getView(long taskId, boolean submitable) {

		return getView(taskId, submitable, null);
	}
	
	public View getView(long taskId, boolean submitable, List<String> preferredTypes) {
		
		List<ViewTaskBind> binds = getBindsDAO().getViewTaskBindsByTaskId(taskId);
		
		if(binds == null || binds.isEmpty()) {
			Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No view task bindings resolved for task. Task id: "+taskId);
			return null;
		}
		
		ViewTaskBind viewTaskBind = null;
		
		if(preferredTypes != null) {
			
			for (String prefferedType : preferredTypes) {
				
				for (ViewTaskBind bind : binds) {
				
					if(prefferedType.equals(bind.getViewType())) {
						viewTaskBind = bind;
						break;
					}
				}
			}
		}
		
		if(viewTaskBind == null)
			viewTaskBind = binds.get(0);
		
		ViewFactory viewFactory;
		String viewType = viewTaskBind.getViewType();
		
		if(getViewTypeFactoryBeanIdentifier().containsKey(viewType)) {
			viewFactory = (ViewFactory)getApplicationContext().getBean(getViewTypeFactoryBeanIdentifier().get(viewType));
			
		} else if(getViewTypeFactoryClass().containsKey(viewType)) {
			
			Object viewFactoryClass = null;
			
			try {
				viewFactoryClass = getViewTypeFactoryClass().get(viewType);
				@SuppressWarnings("unchecked")
				Class<ViewFactory> clazz = (Class<ViewFactory>)viewFactoryClass;
				viewFactory = clazz.newInstance();
				
			} catch (Exception e) {
				throw new RuntimeException("Exception while creating instance of View Factory for class: "+viewFactoryClass, e); 
			}
		} else {
			throw new IllegalStateException("No View Factory registered for view type resolved: "+viewTaskBind+", task id: "+taskId);
		}
		
		return viewFactory.getView(viewTaskBind.getViewIdentifier(), submitable);
	}
	
	protected BPMManagersFactory getManagersCreator(long processDefinitionId) {
		
		String managersType = resolveManagersType(processDefinitionId);
		
		if("default".equals(managersType))
//			TODO: support this
			throw new UnsupportedOperationException("No managers type found for process definition id provided: "+processDefinitionId+". Default managers implementation not supported yet");
		
		BPMManagersFactory creator;
		
		if(getCreatorTypeCreatorBeanIdentifier().containsKey(managersType)) {
			creator = (BPMManagersFactory)getApplicationContext().getBean(creatorTypeCreatorBeanIdentifier.get(managersType));
			
		} else if(getCreatorTypeCreatorClass().containsKey(managersType)) {
			
			Object creatorClass = null;
			
			try {
				creatorClass = creatorTypeCreatorClass.get(managersType);
				@SuppressWarnings("unchecked")
				Class<BPMManagersFactory> clazz = (Class<BPMManagersFactory>)creatorClass;
				creator = clazz.newInstance();
				
			} catch (Exception e) {
				throw new RuntimeException("Exception while creating instance of creator for class: "+creatorClass, e); 
			}
		} else {
			throw new IllegalStateException("No managers creator registered for type resolved: "+managersType+", process definition id: "+processDefinitionId);
		}
		
		return creator;
	}
	
	protected String resolveManagersType(long processDefinitionId) {

		ManagersTypeProcessDefinitionBind bind = getBindsDAO().getManagersTypeProcDefBind(processDefinitionId);
		
		if(bind == null) {
			
			return "default";
		}

		return bind.getManagersType();
	}
	
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		
		if(applicationEvent instanceof BPMManagersFactoryPluggedInEvent) {
			
			BPMManagersFactory managersFactory = ((BPMManagersFactoryPluggedInEvent)applicationEvent).getConcreteBPMManagersCreator();
			
			if(managersFactory.getManagersType() == null)
				throw new IllegalArgumentException("Managers factory type not specified for factory: "+managersFactory);
			
			String beanIdentifier = managersFactory.getBeanIdentifier();
			
			if(beanIdentifier != null) {
				
				getCreatorTypeCreatorBeanIdentifier().put(managersFactory.getManagersType(), beanIdentifier);
				
			} else {
				getCreatorTypeCreatorClass().put(managersFactory.getManagersType(), managersFactory.getClass());
				Logger.getLogger(BPMFactory.class.getName()).log(Level.WARNING, "No Concrete bpm managers creator type was explicitly provided. Skipping.");
			}
		} else if(applicationEvent instanceof ViewFactoryPluggedInEvent) {
			
			ViewFactory viewFactory = ((ViewFactoryPluggedInEvent)applicationEvent).getViewFactory();
			
			if(viewFactory.getViewType() == null)
				throw new IllegalArgumentException("View factory type not specified for factory: "+viewFactory);
			
			String beanIdentifier = viewFactory.getBeanIdentifier();
			
			if(beanIdentifier != null) {
				getViewTypeFactoryBeanIdentifier().put(viewFactory.getViewType(), beanIdentifier);
			} else {
				getViewTypeFactoryClass().put(viewFactory.getViewType(), viewFactory.getClass());
			}
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	protected Map<String, String> getCreatorTypeCreatorBeanIdentifier() {
		
		if(creatorTypeCreatorBeanIdentifier == null)
			creatorTypeCreatorBeanIdentifier = new HashMap<String, String>();
		
		return creatorTypeCreatorBeanIdentifier;
	}

	protected Map<String, Object> getCreatorTypeCreatorClass() {
		
		if(creatorTypeCreatorClass == null)
			creatorTypeCreatorClass = new HashMap<String, Object>(); 
			
		return creatorTypeCreatorClass;
	}

	public BPMDAO getBindsDAO() {
		return bindsDAO;
	}

	@Autowired
	public void setBindsDAO(BPMDAO bindsDAO) {
		this.bindsDAO = bindsDAO;
	}

	public Map<String, String> getViewTypeFactoryBeanIdentifier() {
		
		if(viewTypeFactoryBeanIdentifier == null)
			viewTypeFactoryBeanIdentifier = new HashMap<String, String>();
		
		return viewTypeFactoryBeanIdentifier;
	}

	public Map<String, Object> getViewTypeFactoryClass() {
		
		if(viewTypeFactoryClass == null)
			viewTypeFactoryClass = new HashMap<String, Object>();
		
		return viewTypeFactoryClass;
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

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public RolesManager getRolesManager() {
		return rolesManager;
	}

	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}