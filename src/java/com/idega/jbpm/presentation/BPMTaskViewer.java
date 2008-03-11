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

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.def.View;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/03/11 12:16:59 $ by $Author: civilis $
 */
public class BPMTaskViewer extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "BPMTaskViewer";
	private static final String idegaJbpmContextBeanId = "idegaJbpmContext";
	private static final String bpmFactoryBeanId = "bpmFactory";
	
	
	public static final String PROCESS_DEFINITION_PROPERTY = "processDefinitionId";
//	public static final String PROCESS_INSTANCE_PROPERTY = "processInstanceId";
	public static final String TASK_INSTANCE_PROPERTY = "taskInstanceId";
	public static final String BMPFACTORY_PROPERTY = "bpmFactory";
	public static final String idegaJbpmContext_PROPERTY = "idegaJbpmContext";
	
//	public static final String PROCESS_VIEW_PROPERTY = "processView";
	
	private static final String VIEWER_FACET = "viewer";
	
	private String processDefinitionId;
//	private String processInstanceId;
	private Long taskInstanceId;
//	private boolean processView = false;
	
	private BPMFactory bpmFactory;
	private IdegaJbpmContext idegaJbpmContext;
    
//	public String getProcessInstanceId() {
//		
//		return processInstanceId;
//	}
//	
//	public String getProcessInstanceId(FacesContext context) {
//
//		String processInstanceId = getProcessInstanceId();
//		
//		if(processInstanceId == null) {
//			
//			processInstanceId = getValueBinding(PROCESS_INSTANCE_PROPERTY) != null ? (String)getValueBinding(PROCESS_INSTANCE_PROPERTY).getValue(context) : (String)context.getExternalContext().getRequestParameterMap().get(PROCESS_INSTANCE_PROPERTY);
//			processInstanceId = CoreConstants.EMPTY.equals(processInstanceId) ? null : processInstanceId;
//			setProcessInstanceId(processInstanceId);
//		}
//		
//		return processInstanceId;
//	}
//
//	public void setProcessInstanceId(String processInstanceId) {
//		
//		this.processInstanceId = processInstanceId;
//	}

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
		
		String processDefinitionId = getProcessDefinitionId(context);
//		String processInstanceId = getProcessInstanceId(context);
		Long taskInstanceId = getTaskInstanceId(context);
		
		UIComponent viewer = null;
		
		if(processDefinitionId != null)
			viewer = loadViewerFromDefinition(context, processDefinitionId);
		/*
		else if(processInstanceId != null && isProcessView(context))
			throw new UnsupportedOperationException("TODO: implement");
			//viewer = loadFormViewerForProcessView(context, processInstanceId);
		else if(processInstanceId != null)
			viewer = loadViewerFromInstance(context, processInstanceId);
		*/
		else if(taskInstanceId != null)
			viewer = loadViewerFromTaskInstance(context, taskInstanceId);
		
		@SuppressWarnings("unchecked")
		Map<String, UIComponent> facets = (Map<String, UIComponent>)getFacets();
		
		if(viewer != null)
			facets.put(VIEWER_FACET, viewer);
		else
			facets.remove(VIEWER_FACET);
	}
	
	private UIComponent loadViewerFromDefinition(FacesContext context, String processDefinitionId) {

		int initiatorId = IWContext.getIWContext(context).getCurrentUserId();
		long pdId = Long.parseLong(processDefinitionId);
		
		View initView = getBpmFactory(context).getViewManager(pdId).loadInitView(pdId, initiatorId, context);
		return initView.getViewForDisplay();
	}

	/*
	 * private UIComponent loadViewerFromInstance(FacesContext context, String processInstanceId) {

//		TODO: get task instance id to display
		View initView = getProcess(context).getViewManager().loadTaskInstanceView(context, null);
		return initView.getViewForDisplay();
	}
	 
	 */
	
	
	private UIComponent loadViewerFromTaskInstance(FacesContext context, Long taskInstanceId) {
		
		JbpmContext ctx = getIdegaJbpmContext(context).createJbpmContext();
		
		try {
			long pdId = ctx.getTaskInstance(taskInstanceId).getProcessInstance().getProcessDefinition().getId();
			View initView = getBpmFactory(context).getViewManager(pdId).loadTaskInstanceView(taskInstanceId, context);
			return initView.getViewForDisplay();
		
		} catch (AccessControlException e) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No access");
			return null;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
//	private FormViewer loadFormViewerForProcessView(FacesContext context, String processInstanceId) {
//
//		Document xformsDoc = getFormManager(context).loadProcessViewForm(context, Long.parseLong(processInstanceId), IWContext.getIWContext(context).getCurrentUserId());
//		
//		FormViewer formviewer = new FormViewer();
//		formviewer.setRendered(true);
//		formviewer.setXFormsDocument(xformsDoc);
//		return formviewer;
//	}
	
	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		
		super.encodeChildren(context);

		@SuppressWarnings("unchecked")
		Map<String, UIComponent> facets = (Map<String, UIComponent>)getFacets();
		UIComponent viewer = facets.get(VIEWER_FACET);
		
		if(viewer != null)
			renderChild(context, viewer);
	}

//	public boolean isProcessView() {
//		return processView;
//	}
//
//	public void setProcessView(boolean processView) {
//		this.processView = processView;
//	}
	
	/*
	public boolean isProcessView(FacesContext context) {

		boolean isProcessView = isProcessView();
		
		if(!isProcessView) {
			
			if(getValueBinding(PROCESS_VIEW_PROPERTY) != null) {
				
				isProcessView = (Boolean)getValueBinding(PROCESS_VIEW_PROPERTY).getValue(context);
			} else {
				Object requestParam = context.getExternalContext().getRequestParameterMap().get(PROCESS_VIEW_PROPERTY);
				
				if(requestParam instanceof Boolean)
					isProcessView = (Boolean)isProcessView;
				else
					isProcessView = "1".equals(requestParam);
			}
			setProcessView(isProcessView);
		}
		
		return isProcessView;
	}
	*/

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
	
	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}
	
	public IdegaJbpmContext getIdegaJbpmContext(FacesContext context) {
		
		IdegaJbpmContext ctx = getIdegaJbpmContext();
		if(ctx == null) {
			
			ctx = getValueBinding(idegaJbpmContext_PROPERTY) != null ? (IdegaJbpmContext)getValueBinding(idegaJbpmContext_PROPERTY).getValue(context) : null;
			
			if(ctx == null)
				ctx = (IdegaJbpmContext)getBeanInstance(idegaJbpmContextBeanId);
			
			setIdegaJbpmContext(ctx);
		}
		
		return ctx;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
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
}