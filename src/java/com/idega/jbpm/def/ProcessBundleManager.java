package com.idega.jbpm.def;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.identity.RolesManager;
import com.idega.jbpm.identity.permission.RoleScope;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.15 $
 * 
 * Last modified: $Date: 2008/05/06 21:42:49 $ by $Author: civilis $
 */
@Scope("prototype")
@Service
public class ProcessBundleManager {

	private BPMDAO bpmBindsDAO;
	private IdegaJbpmContext idegaJbpmContext;
	private RolesManager rolesManager;
	
	public long createBundle(ProcessBundle processBundle,
			String processDefinitionName, IWMainApplication iwma) throws IOException {
		
		return createBundle(processBundle, processDefinitionName, iwma, null);
	}

	/**
	 * 
	 * @param processBundle bundle to create process bundle from. i.e. all the resources, like process definition and views
	 * @param processDefinitionName - optional
	 * @return process definition id, of the created bundle
	 * @throws IOException
	 */
	public long createBundle(ProcessBundle processBundle,
			String processDefinitionName, IWMainApplication iwma, Integer version) throws IOException {

		String managersType = processBundle.getManagersType();
		
		if(managersType == null || CoreConstants.EMPTY.equals(managersType))
			throw new IllegalArgumentException("No managers type in process bundle provided: "+processBundle.getClass().getName());
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

		try {

			ProcessDefinition pd = processBundle.getProcessDefinition();

			if (processDefinitionName != null)
				pd.setName(processDefinitionName);
			
			if(version != null)
				pd.setVersion(version);
			
			ctx.getGraphSession().deployProcessDefinition(pd);

			try {

				@SuppressWarnings("unchecked")
				Collection<Task> tasks = pd.getTaskMgmtDefinition().getTasks()
						.values();
				
				ViewToTask viewToTaskBinder = processBundle.getViewToTaskBinder();

				for (Task task : tasks) {

					List<ViewResource> viewResources = processBundle
							.getViewResources(task.getName());
					
					if(viewResources != null) {
					
						for (ViewResource viewResource : viewResources) {

							View view = viewResource.store(iwma);
							viewToTaskBinder.bind(view, task);
						}
					} else {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "No view resources resolved for task: "+task.getId());
					}
				}
				
				ManagersTypeProcessDefinitionBind mtpdb = new ManagersTypeProcessDefinitionBind();
				mtpdb.setManagersType(managersType);
				mtpdb.setProcessDefinitionId(pd.getId());
				getBpmBindsDAO().persist(mtpdb);
				
				createProcessRoles(pd);
				createTasksPermissions(pd);
				
				processBundle.configure(pd);

			} catch (Exception e) {

				Logger.getLogger(getClass().getName()).log(Level.SEVERE,
						"Exception while storing views and binding with tasks",
						e);
				// TODO: remove all binds and views too
				ctx.getGraphSession().deleteProcessDefinition(pd);
			}

			return pd.getId();

		} finally {

			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected void createProcessRoles(ProcessDefinition pd) {
		
		@SuppressWarnings("unchecked")
		Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();

		ArrayList<Role> rolesToCreate = new ArrayList<Role>();
		
		for (Task task : tasks.values()) {
			
			String jsonExp = task.getAssignmentDelegation().getConfiguration();
			
			List<Role> roles = JSONExpHandler.resolveRolesFromJSONExpression(jsonExp);
			
			for (Role role : roles) {
			
				if(role.getScope() == RoleScope.PD)
					rolesToCreate.add(role);
			}
		}
		
		if(!rolesToCreate.isEmpty())
			getRolesManager().createProcessRoles(pd.getName(), rolesToCreate);
	}
	
	protected void createTasksPermissions(ProcessDefinition pd) {
		
		@SuppressWarnings("unchecked")
		Map<String, Task> tasks = pd.getTaskMgmtDefinition().getTasks();

		for (Task task : tasks.values()) {
			
			String jsonExp = task.getAssignmentDelegation().getConfiguration();
			
			List<Role> roles = JSONExpHandler.resolveRolesFromJSONExpression(jsonExp);
			getRolesManager().createTaskRolesPermissionsPDScope(task, roles);
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
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