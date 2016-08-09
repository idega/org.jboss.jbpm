package com.idega.jbpm.presentation;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.BPMContext;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.exe.ProcessConstants;
import com.idega.jbpm.exe.ProcessDefinitionW;
import com.idega.jbpm.exe.ProcessManager;
import com.idega.jbpm.exe.TaskInstanceW;
import com.idega.jbpm.view.View;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.presentation.Page;
import com.idega.util.CoreConstants;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.25 $
 *
 *          Last modified: $Date: 2009/06/08 14:27:35 $ by $Author: valdas $
 */
public class BPMTaskViewer extends IWBaseComponent {

	public static final String COMPONENT_TYPE = "BPMTaskViewer";

	public static final String PROCESS_DEFINITION_PROPERTY = "processDefinitionId";
	public static final String TASK_INSTANCE_PROPERTY = ProcessConstants.TASK_INSTANCE_ID;

	private static final String VIEWER_FACET = "viewer";

	private String processName;
	private Long processDefinitionId;
	private Long taskInstanceId;

	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private BPMContext bpmContext;

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
		Long processDefinitionId = getProcessDefinitionId(context);
		Long taskInstanceId = getTaskInstanceId(context);

		UIComponent viewer = null;

		if (!StringUtil.isEmpty(processName)) {
			viewer = loadViewerFromProcessName(context, processName);
		} else if (processDefinitionId != null) {
			viewer = loadViewerFromDefinition(context, processDefinitionId, IWContext.getIWContext(context).getApplicationSettings().getBoolean("xform_load_latest", Boolean.TRUE));
		} else if (taskInstanceId != null) {
			viewer = loadViewerFromTaskInstance(context, taskInstanceId);
		}

		Map<String, UIComponent> facets = getFacets();

		if (viewer == null) {
			facets.remove(VIEWER_FACET);
		} else {
			facets.put(VIEWER_FACET, viewer);
		}
	}

	private UIComponent getView(IWContext iwc, ProcessDefinitionW procDef, Integer initiatorId) {
		if (!procDef.isAvailable(iwc)) {
			String url = procDef.getNotAvailableLink(iwc);
			if (StringUtil.isEmpty(url)) {
				url = CoreConstants.PAGES_URI_PREFIX;
				getLogger().warning("Proc. def. " + procDef.getProcessDefinitionId() + " is not available, but redirect URL is not available! Redirecting to default location: " + CoreConstants.PAGES_URI_PREFIX);
			} else {
				getLogger().info("Proc. def. " + procDef.getProcessDefinitionId() + " is not available. Redirecting to " + url);
			}

			iwc.sendRedirect(url);
			return null;
		}

		setPageTitle(procDef.getProcessName(iwc.getCurrentLocale()));

		View initView = procDef.loadInitView(initiatorId);
		return initView.getViewForDisplay();
	}

	private UIComponent loadViewerFromDefinition(FacesContext context, Long processDefinitionId, boolean checkVersion) {
		final IWContext iwc = IWContext.getIWContext(context);
		final Integer initiatorId;

		if (iwc.isLoggedOn()) {
			initiatorId = iwc.getCurrentUserId();
		} else {
			initiatorId = null;
		}

		ProcessManager pm = getBpmFactory().getProcessManager(processDefinitionId);
		ProcessDefinitionW procDef = pm.getProcessDefinition(processDefinitionId);

		if (checkVersion) {
			ProcessDefinitionW latestProcDef = pm.getProcessDefinition(procDef.getProcessDefinition().getName());
			Long latestProcDefId = latestProcDef.getProcessDefinitionId();
			if (latestProcDefId.longValue() != processDefinitionId.longValue()) {
				setProcessDefinitionId(latestProcDefId);
				getLogger().info("Provided proc. def. ID (" + processDefinitionId + ") is not the latest version (" + latestProcDefId + "), will load latest version");
				return loadViewerFromDefinition(context, latestProcDefId, false);
			}
		}

		return getView(iwc, procDef, initiatorId);
	}

	private void setPageTitle(String title) {
		if (StringUtil.isEmpty(title)) {
			return;
		}

		Page page = getParentPage();
		if (page != null) {
			page.setTitle(title);
		}
	}

	private UIComponent loadViewerFromProcessName(FacesContext context, String processName) {
		final IWContext iwc = IWContext.getIWContext(context);
		final Integer initiatorId;

		PresentationUtil.addJavaScriptActionToBody(iwc, "var BPMConfiguration = {}; BPMConfiguration.processName = '"+processName+"';");

		if (iwc.isLoggedOn()) {
			initiatorId = iwc.getCurrentUserId();
		} else {
			initiatorId = null;
		}

		ProcessDefinitionW procDef = getBpmFactory().getProcessManager(processName).getProcessDefinition(processName);
		return getView(iwc, procDef, initiatorId);
	}

	private UIComponent loadViewerFromTaskInstance(FacesContext context, Long taskInstanceId) {
		try {
			TaskInstanceW tiW = getBpmFactory().getProcessManagerByTaskInstanceId(taskInstanceId).getTaskInstance(taskInstanceId);
			setPageTitle(tiW.getName(IWContext.getIWContext(context).getCurrentLocale()));
			View initView = tiW.loadView();
			initView.setSubmitted(tiW.isSubmitted());

			return initView.getViewForDisplay();
		} catch (AccessControlException e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No access to task " + taskInstanceId);
			return null;
		}
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		super.encodeChildren(context);

		Map<String, UIComponent> facets = getFacets();
		UIComponent viewer = facets.get(VIEWER_FACET);

		if (viewer != null) {
			renderChild(context, viewer);
		}
	}

	public Long getTaskInstanceId(FacesContext context) {
		Long taskInstanceId = getTaskInstanceId();
		if (taskInstanceId == null) {
			taskInstanceId = getExpressionValue(context, TASK_INSTANCE_PROPERTY);

			if (taskInstanceId == null && context.getExternalContext().getRequestParameterMap().containsKey(TASK_INSTANCE_PROPERTY)) {
				String val = context.getExternalContext().getRequestParameterMap().get(TASK_INSTANCE_PROPERTY);
				if (!StringUtil.isEmpty(val)) {
					taskInstanceId = new Long(val);
				}
			}

			setTaskInstanceId(taskInstanceId);
		}

		return taskInstanceId;
	}

	public BPMFactory getBpmFactory() {
		if (bpmFactory == null) {
			ELUtil.getInstance().autowire(this);
		}

		return bpmFactory;
	}

	public BPMContext getBpmContext() {
		if (bpmContext == null) {
			ELUtil.getInstance().autowire(this);
		}

		return bpmContext;
	}

	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public Long getProcessDefinitionId(FacesContext context) {
		Long processDefinitionId = getProcessDefinitionId();
		if (processDefinitionId == null) {
			processDefinitionId = getExpressionValue(context, PROCESS_DEFINITION_PROPERTY);

			if (processDefinitionId == null && context.getExternalContext().getRequestParameterMap().containsKey(PROCESS_DEFINITION_PROPERTY)) {
				String val = context.getExternalContext().getRequestParameterMap().get(PROCESS_DEFINITION_PROPERTY);
				if (!StringUtil.isEmpty(val)) {
					processDefinitionId = new Long(val);
				}
			}

			setProcessDefinitionId(processDefinitionId);
		}

		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
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