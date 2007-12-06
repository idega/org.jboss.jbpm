package com.idega.jbpm.presentation;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import com.idega.jbpm.def.View;
import com.idega.jbpm.exe.Process;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;

/**
 * TODO: move this to jbpm module
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/12/06 13:26:17 $ by $Author: civilis $
 */
public class ProcessViewer extends IWBaseComponent {
	
	public static final String COMPONENT_TYPE = "ProcessViewer";
	
	public static final String PROCESS_DEFINITION_PROPERTY = "processDefinitionId";
	public static final String PROCESS_INSTANCE_PROPERTY = "processInstanceId";
	public static final String TASK_INSTANCE_PROPERTY = "taskInstanceId";
	public static final String PROCESS_PROPERTY = "process";
	public static final String PROCESS_VIEW_PROPERTY = "processView";
	
	private static final String VIEWER_FACET = "viewer";
	
	private String processDefinitionId;
	private String processInstanceId;
	private String taskInstanceId;
	private boolean processView = false;
	
	private Process process;
    
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

	public String getProcessInstanceId() {
		
		return processInstanceId;
	}
	
	public String getProcessInstanceId(FacesContext context) {

		String processInstanceId = getProcessInstanceId();
		
		if(processInstanceId == null) {
			
			processInstanceId = getValueBinding(PROCESS_INSTANCE_PROPERTY) != null ? (String)getValueBinding(PROCESS_INSTANCE_PROPERTY).getValue(context) : (String)context.getExternalContext().getRequestParameterMap().get(PROCESS_INSTANCE_PROPERTY);
			processInstanceId = CoreConstants.EMPTY.equals(processInstanceId) ? null : processInstanceId;
			setProcessInstanceId(processInstanceId);
		}
		
		return processInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		
		this.processInstanceId = processInstanceId;
	}

	public ProcessViewer() {
		
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
		String processInstanceId = getProcessInstanceId(context);
		String taskInstanceId = getTaskInstanceId(context);
		
		UIComponent viewer = null;
		
		if(processDefinitionId != null)
			viewer = loadViewerFromDefinition(context, processDefinitionId);
		else if(processInstanceId != null && isProcessView(context))
			throw new UnsupportedOperationException("TODO: implement");
			//viewer = loadFormViewerForProcessView(context, processInstanceId);
		else if(processInstanceId != null)
			viewer = loadViewerFromInstance(context, processInstanceId);
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
		
		View initView = getProcess(context).getViewManager().loadInitView(context, Long.parseLong(processDefinitionId), initiatorId);
		return initView.getViewForDisplay();
	}
	
	private UIComponent loadViewerFromInstance(FacesContext context, String processInstanceId) {

//		TODO: get task instance id to display
		View initView = getProcess(context).getViewManager().loadTaskInstanceView(context, null);
		return initView.getViewForDisplay();
	}
	
	private UIComponent loadViewerFromTaskInstance(FacesContext context, String taskInstanceId) {

		View initView = getProcess(context).getViewManager().loadTaskInstanceView(context, Long.parseLong(taskInstanceId));
		return initView.getViewForDisplay();
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

	public boolean isProcessView() {
		return processView;
	}

	public void setProcessView(boolean processView) {
		this.processView = processView;
	}
	
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

	public String getTaskInstanceId() {
		return taskInstanceId;
	}
	
	public String getTaskInstanceId(FacesContext context) {

		String taskInstanceId = getTaskInstanceId();
		
		if(taskInstanceId == null) {
			
			taskInstanceId = getValueBinding(TASK_INSTANCE_PROPERTY) != null ? (String)getValueBinding(TASK_INSTANCE_PROPERTY).getValue(context) : (String)context.getExternalContext().getRequestParameterMap().get(TASK_INSTANCE_PROPERTY);
			taskInstanceId = CoreConstants.EMPTY.equals(taskInstanceId) ? null : taskInstanceId;
			setTaskInstanceId(taskInstanceId);
		}
		
		return taskInstanceId;
	}

	public void setTaskInstanceId(String taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}

	public Process getProcess() {
		return process;
	}
	
	public Process getProcess(FacesContext context) {
		
		Process process = getProcess();
		if(process == null) {
			
			process = getValueBinding(PROCESS_PROPERTY) != null ? (Process)getValueBinding(PROCESS_PROPERTY).getValue(context) : null;
			setProcess(process);
		}
		
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}
}