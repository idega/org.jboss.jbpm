package com.idega.jbpm.bundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.instantiation.Delegation;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.business.IBOLookup;
import com.idega.core.accesscontrol.business.StandardRoles;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.TaskAssignment;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewToTask;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.AccessControlList;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.17 $ Last modified: $Date: 2009/02/20 14:24:52 $ by
 *          $Author: civilis $
 */
@Scope("prototype")
@Service
@Transactional
public class ProcessBundleManager {

	@Autowired
	private BPMDAO bpmBindsDAO;
	@Autowired
	private BPMContext idegaJbpmContext;
	@Autowired
	private BPMFactory bpmFactory;
	@Autowired
	private ViewToTask viewToTask;

	/**
	 * 
	 * @param processBundle
	 *            bundle to create process bundle from. i.e. all the resources,
	 *            like process definition and views
	 * @param processDefinitionName
	 *            - optional
	 * @return process definition id, of the created bundle
	 * @throws IOException
	 */
	public long createBundle(final ProcessBundle processBundle,
			final IWMainApplication iwma) throws IOException {

		final String processManagerType = processBundle.getProcessManagerType();

		if (StringUtil.isEmpty(processManagerType))
			throw new IllegalArgumentException(
					"No process mmanager type in process bundle provided: "
							+ processBundle.getClass().getName());

		Long processDefinitionId = getBPMContext().execute(new JbpmCallback() {

			public Object doInJbpm(JbpmContext context) throws JbpmException {

				ProcessDefinition pd = null;

				try {
					pd = processBundle.getProcessDefinition();

					context.getGraphSession().deployProcessDefinition(pd);
					
					TaskMgmtDefinition taskMgmtDef = pd.getTaskMgmtDefinition();
					
					@SuppressWarnings("unchecked")
					Map<String, Task> tasksMap = taskMgmtDef.getTasks();
					final String processName = pd.getName();
					
					if(tasksMap != null) {
						
						@SuppressWarnings("unchecked")
						Collection<Task> tasks = pd.getTaskMgmtDefinition()
								.getTasks().values();

						for (Task task : tasks) {

							List<ViewResource> viewResources = processBundle
									.getViewResources(task.getName());

							if (viewResources != null) {

								for (ViewResource viewResource : viewResources) {

									viewResource.setProcessName(processName);
									viewResource.store(iwma);
									getViewToTask().bind(viewResource.getViewId(),
											viewResource.getViewType(), task);
								}
							} else {
								Logger.getLogger(getClass().getName()).log(
										Level.WARNING,
										"No view resources resolved for task: "
												+ task.getId());
							}
						}
					}

					if (getBPMDAO().getProcessManagerBind(processName) == null) {

						ProcessManagerBind pmb = new ProcessManagerBind();
						pmb.setManagersType(processManagerType);
						pmb.setProcessName(processName);
						getBPMDAO().persist(pmb);
					}

					createProcessRoles(pd);
					// createTasksPermissions(pd);

					processBundle.configure(pd);

					return pd.getId();

				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (Exception e) {

					Logger
							.getLogger(getClass().getName())
							.log(Level.SEVERE,
									"Exception while storing views and binding with tasks");
					// TODO: rollback here if hibernate doesn't do it?

					throw new RuntimeException(e);
				}
			}
		});

		// TODO: catch RuntimeException with cause of IOException (perhaps make
		// some wrapper), and throw the original IOException here

		return processDefinitionId;
	}

	protected void createProcessRoles(ProcessDefinition pd) {

		@SuppressWarnings("unchecked")
		Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();
		
		if(tasks != null) {
			
			ArrayList<Role> rolesToCreate = new ArrayList<Role>();

			for (Task task : tasks.values()) {

				// TODO: fix and adjust this. The notation changed.

				Delegation deleg = task.getAssignmentDelegation();

				if (deleg != null && deleg.getConfiguration() != null) {

					String jsonExp = deleg.getConfiguration();

					// skipping scripted expressions, which should be evaluated at
					// runtime
					if (!jsonExp.contains("${") && !jsonExp.contains("#{")) {

						TaskAssignment ta = JSONExpHandler
								.resolveRolesFromJSONExpression(jsonExp);

						List<Role> roles = ta.getRoles();

						for (Role role : roles) {

							if (role.getScope() == RoleScope.PD)
								rolesToCreate.add(role);
						}
					}
				}
			}

			if (!rolesToCreate.isEmpty()) {

				getBpmFactory().getRolesManager()
						.createNativeRolesFromProcessRoles(pd.getName(),
								rolesToCreate);

				// TODO: move this to appropriate place, also store path of the
				// process should be
				// accessible through api

				String storePath = new StringBuilder(JBPMConstants.BPM_PATH)
						.append(CoreConstants.SLASH).append(pd.getName())
						.toString();

				ArrayList<String> roleNames = new ArrayList<String>(rolesToCreate
						.size());

				for (Role role : rolesToCreate) {
					roleNames.add(role.getRoleName());
				}
				
//				TODO: this need to be outside if, i.e. admin still should have access to the folder, even if process doesn't have any roles
				roleNames.add(StandardRoles.ROLE_KEY_ADMIN);

				try {
					IWSlideService slideService = (IWSlideService) IBOLookup
							.getServiceInstance(IWMainApplication
									.getDefaultIWApplicationContext(),
									IWSlideService.class);

					AccessControlList processFolderACL = slideService
							.getAccessControlList(storePath);

					processFolderACL = slideService.getAuthenticationBusiness()
							.applyPermissionsToRepository(processFolderACL,
									roleNames);

					slideService.storeAccessControlList(processFolderACL);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * TODO is this still needed (useful) protected void
	 * createTasksPermissions(ProcessDefinition pd) {
	 * 
	 * @SuppressWarnings("unchecked") Map<String, Task> tasks =
	 * pd.getTaskMgmtDefinition().getTasks(); RolesManager rolesManager =
	 * getBpmFactory().getRolesManager();
	 * 
	 * for (Task task : tasks.values()) {
	 * 
	 * Delegation deleg = task.getAssignmentDelegation();
	 * 
	 * if (deleg != null && deleg.getConfiguration() != null) {
	 * 
	 * String jsonExp = deleg.getConfiguration();
	 * 
	 * TaskAssignment ta = JSONExpHandler
	 * .resolveRolesFromJSONExpression(jsonExp); List<Role> roles =
	 * ta.getRoles();
	 * 
	 * rolesManager.createTaskRolesPermissions(task, roles); } } }
	 */

	BPMContext getBPMContext() {
		return idegaJbpmContext;
	}

	BPMDAO getBPMDAO() {
		return bpmBindsDAO;
	}

	ViewToTask getViewToTask() {
		return viewToTask;
	}

	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
}