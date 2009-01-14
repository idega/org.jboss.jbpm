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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.TaskAssignment;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.jbpm.view.View;
import com.idega.jbpm.view.ViewResource;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.AccessControlList;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $ Last modified: $Date: 2009/01/14 14:26:01 $ by $Author: civilis $
 */
@Scope("prototype")
@Service
public class ProcessBundleManager {
	
	private BPMDAO bpmBindsDAO;
	private BPMContext idegaJbpmContext;
	private RolesManager rolesManager;
	
	/**
	 * @param processBundle
	 *            bundle to create process bundle from. i.e. all the resources, like process
	 *            definition and views
	 * @param processDefinitionName
	 *            - optional
	 * @return process definition id, of the created bundle
	 * @throws IOException
	 */
	public long createBundle(final ProcessBundle processBundle,
	        final IWMainApplication iwma) throws IOException {
		
		final String managersType = processBundle.getManagersType();
		
		if (StringUtil.isEmpty(managersType))
			throw new IllegalArgumentException(
			        "No managers type in process bundle provided: "
			                + processBundle.getClass().getName());
		
		Long processDefinitionId = getIdegaJbpmContext().execute(
		    new JbpmCallback() {
			    
			    public Object doInJbpm(JbpmContext context)
			            throws JbpmException {
				    
				    ProcessDefinition pd = null;
				    
				    try {
					    pd = processBundle.getProcessDefinition();
					    
					    context.getGraphSession().deployProcessDefinition(pd);
					    
					    @SuppressWarnings("unchecked")
					    Collection<Task> tasks = pd.getTaskMgmtDefinition()
					            .getTasks().values();
					    
					    final String processName = pd.getName();
					    
					    for (Task task : tasks) {
						    
						    List<ViewResource> viewResources = processBundle
						            .getViewResources(task.getName());
						    
						    if (viewResources != null) {
							    
							    for (ViewResource viewResource : viewResources) {
								    
								    viewResource.setProcessName(processName);
								    View view = viewResource.store(iwma);
								    view.getViewToTask().bind(view, task);
							    }
						    } else {
							    Logger.getLogger(getClass().getName()).log(
							        Level.WARNING,
							        "No view resources resolved for task: "
							                + task.getId());
						    }
					    }
					    
					    if (getBpmBindsDAO().getProcessManagerBind(processName) == null) {
						    
						    ProcessManagerBind pmb = new ProcessManagerBind();
						    pmb.setManagersType(managersType);
						    pmb.setProcessName(processName);
						    getBpmBindsDAO().persist(pmb);
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
					    // TODO: remove all binds and views too
					    
					    if (pd != null)
						    context.getGraphSession().deleteProcessDefinition(
						        pd);
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
			
			getRolesManager().createNativeRolesFromProcessRoles(pd.getName(),
			    rolesToCreate);
			
			// TODO: move this to appropriate place, also store path of the process should be
			// accessible through api
			
			String storePath = new StringBuilder(JBPMConstants.BPM_PATH)
			        .append(CoreConstants.SLASH).append(pd.getName())
			        .toString();
			
			ArrayList<String> roleNames = new ArrayList<String>(rolesToCreate
			        .size());
			
			for (Role role : rolesToCreate) {
				roleNames.add(role.getRoleName());
			}
			
			try {
				IWSlideService slideService = (IWSlideService) IBOLookup
				        .getServiceInstance(IWMainApplication
				                .getDefaultIWApplicationContext(),
				            IWSlideService.class);
				// slideService
				// .createAllFoldersInPathAsRoot(CoreConstants.CONTENT_PATH);
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
	
	protected void createTasksPermissions(ProcessDefinition pd) {
		
		@SuppressWarnings("unchecked")
		Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();
		
		for (Task task : tasks.values()) {
			
			Delegation deleg = task.getAssignmentDelegation();
			
			if (deleg != null && deleg.getConfiguration() != null) {
				
				String jsonExp = deleg.getConfiguration();
				
				TaskAssignment ta = JSONExpHandler
				        .resolveRolesFromJSONExpression(jsonExp);
				List<Role> roles = ta.getRoles();
				
				getRolesManager().createTaskRolesPermissions(task, roles);
			}
		}
	}
	
	public BPMContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}
	
	@Autowired
	public void setIdegaJbpmContext(BPMContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	public BPMDAO getBpmBindsDAO() {
		return bpmBindsDAO;
	}
	
	@Autowired
	public void setBpmBindsDAO(BPMDAO bpmBindsDAO) {
		this.bpmBindsDAO = bpmBindsDAO;
	}
	
	public RolesManager getRolesManager() {
		return rolesManager;
	}
	
	@Autowired
	public void setRolesManager(RolesManager rolesManager) {
		this.rolesManager = rolesManager;
	}
}