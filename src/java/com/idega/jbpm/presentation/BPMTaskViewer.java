package com.idega.jbpm.presentation;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.jbpm.JbpmContext;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.21 $
 *
 * Last modified: $Date: 2008/12/13 15:45:06 $ by $Author: civilis $
 */
public class BPMTaskViewer extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "BPMTaskViewer";
	private static final String idegaJbpmContextBeanId = "idegaJbpmContext";
	private static final String bpmFactoryBeanId = "bpmFactory";
	
	public static final String PROCESS_DEFINITION_PROPERTY = "processDefinitionId";
	public static final String TASK_INSTANCE_PROPERTY = "taskInstanceId";
	public static final String BMPFACTORY_PROPERTY = "bpmFactory";
	public static final String idegaJbpmContext_PROPERTY = "idegaJbpmContext";
	
	private static final String VIEWER_FACET = "viewer";

	private String processName;
	private String processDefinitionId;
	private Long taskInstanceId;
	
	private BPMFactory bpmFactory;
	private BPMContext idegaJbpmContext;
    
	public BPMTaskViewer() {
		
		super();
		setRendererType(null);
	}
	
	@Override
	public boolean getRendersChildren() {
		return true;
	}
	
	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		
		super.encodeBegin(context);
		
		String processName = getProcessName();
		String processDefinitionId = getProcessDefinitionId(context);
		Long taskInstanceId = getTaskInstanceId(context);
		
		UIComponent viewer = null;
		
		if(processName != null && !CoreConstants.EMPTY.equals(processName))
			viewer = loadViewerFromProcessName(context, processName);
		else if(processDefinitionId != null && !CoreConstants.EMPTY.equals(processDefinitionId))
			viewer = loadViewerFromDefinition(context, processDefinitionId);
		else if(taskInstanceId != null)
			viewer = loadViewerFromTaskInstance(context, taskInstanceId);
		
		Map<String, UIComponent> facets = getFacets();
		
		if(viewer != null)
			facets.put(VIEWER_FACET, viewer);
		else
			facets.remove(VIEWER_FACET);
	}
	
	private UIComponent loadViewerFromDefinition(FacesContext context, String processDefinitionId) {

		final IWContext iwc = IWContext.getIWContext(context);
		final Integer initiatorId;
		
		if(iwc.isLoggedOn())
			initiatorId = IWContext.getIWContext(context).getCurrentUserId();
		else
			initiatorId = null;
		
		long pdId = Long.parseLong(processDefinitionId);
		
		View initView = getBpmFactory(context).getProcessManager(pdId).getProcessDefinition(pdId).loadInitView(initiatorId);
		return initView.getViewForDisplay();
	}
	
	private UIComponent loadViewerFromProcessName(FacesContext context, String processName) {

		final IWContext iwc = IWContext.getIWContext(context);
		final Integer initiatorId;
		
		if(iwc.isLoggedOn())
			initiatorId = IWContext.getIWContext(context).getCurrentUserId();
		else
			initiatorId = null;
		
		View initView = getBpmFactory(context).getProcessManager(processName).getProcessDefinition(processName).loadInitView(initiatorId);
		return initView.getViewForDisplay();
	}

	private UIComponent loadViewerFromTaskInstance(FacesContext context, Long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext(context).createJbpmContext();
		
		try {
			long pdId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			View initView = getBpmFactory(context).getProcessManager(pdId).getTaskInstance(taskInstanceId).loadView();
			return initView.getViewForDisplay();
		
		} catch (AccessControlException e) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No access");
			return null;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		
		super.encodeChildren(context);

		Map<String, UIComponent> facets = getFacets();
		UIComponent viewer = facets.get(VIEWER_FACET);
		
		if(viewer != null)
			renderChild(context, viewer);
	}

	public Long getTaskInstanceId(FacesContext context) {

		Long taskInstanceId = getTaskInstanceId();
		
		if(taskInstanceId == null) {
			
			ValueBinding binding = getValueBinding(TASK_INSTANCE_PROPERTY);
			
			
			if(binding != null && binding.getValue(context) != null)
				taskInstanceId = (Long)binding.getValue(context);
			
			else if(context.getExternalContext().getRequestParameterMap().containsKey(TASK_INSTANCE_PROPERTY)) {
				
				Object val = context.getExternalContext().getRequestParameterMap().get(TASK_INSTANCE_PROPERTY);
				
				if(val instanceof Long)
					taskInstanceId = (Long)val;
				else if((val instanceof String) && !CoreConstants.EMPTY.equals(val))
					taskInstanceId = new Long((String)val);
			}
			
			setTaskInstanceId(taskInstanceId);
		}

		return taskInstanceId;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public BPMFactory getBpmFactory(FacesContext context) {
		
		BPMFactory bpmFactory = getBpmFactory();
		if(bpmFactory == null) {
			
			bpmFactory = getValueBinding(BMPFACTORY_PROPERTY) != null ? (BPMFactory)getValueBinding(BMPFACTORY_PROPERTY).getValue(context) : null;
			
			if(bpmFactory == null)
				bpmFactory = (BPMFactory)getBeanInstance(bpmFactoryBeanId);
			
			setBpmFactory(bpmFactory);
		}
		
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}
	
	public BPMContext getIdegaJbpmContext(FacesContext context) {
		
		BPMContext ctx = getIdegaJbpmContext();
		if(ctx == null) {
			
			ctx = getValueBinding(idegaJbpmContext_PROPERTY) != null ? (IdegaJbpmContext)getValueBinding(idegaJbpmContext_PROPERTY).getValue(context) : null;
			
			if(ctx == null)
				ctx = (BPMContext)getBeanInstance(idegaJbpmContextBeanId);
			
			setIdegaJbpmContext(ctx);
		}
		
		return ctx;
	}

	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public String getProcessDefinitionId() {
		
		return processDefinitionId;
	}
	
	public String getProcessDefinitionId(FacesContext context) {

		String processDefinitionId = getProcessDefinitionId();
		
		if(processDefinitionId == null) {
			
			processDefinitionId = getValueBinding(PROCESS_DEFINITION_PROPERTY) != null ? (String)getValueBinding(PROCESS_DEFINITION_PROPERTY).getValue(context) : (String)context.getExternalContext().getRequestParameterMap().get(PROCESS_DEFINITION_PROPERTY);
			processDefinitionId = CoreConstants.EMPTY.equals(processDefinitionId) ? null : processDefinitionId;
			setProcessDefinitionId(processDefinitionId);
		}
		
		return processDefinitionId;
	}

	public void setProcessDefinitionId(String processDefinitionId) {
		
		this.processDefinitionId = processDefinitionId;
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}
}