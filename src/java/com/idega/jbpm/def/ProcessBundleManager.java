package com.idega.jbpm.def;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.ManagersTypeProcessDefinitionBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 * 
 * Last modified: $Date: 2008/03/11 12:16:59 $ by $Author: civilis $
 */
public class ProcessBundleManager {

	private ViewToTask viewToTaskBinder;
	private BPMDAO bpmBindsDAO;
	private IdegaJbpmContext idegaJbpmContext;

	public ViewToTask getViewToTaskBinder() {
		return viewToTaskBinder;
	}

	public void setViewToTaskBinder(ViewToTask viewToTaskBinder) {
		this.viewToTaskBinder = viewToTaskBinder;
	}

	/**
	 * 
	 * @param processBundle bundle to create process bundle from. i.e. all the resources, like process definition and views
	 * @param processDefinitionName - optional
	 * @return process definition id, of the created bundle
	 * @throws IOException
	 */
	public long createBundle(ProcessBundle processBundle,
			String processDefinitionName) throws IOException {

		String managersType = processBundle.getManagersType();
		
		if(managersType == null || CoreConstants.EMPTY.equals(managersType))
			throw new IllegalArgumentException("No managers type in process bundle provided: "+processBundle.getClass().getName());
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();

		try {

			ProcessDefinition pd = processBundle.getProcessDefinition();

			if (processDefinitionName != null)
				pd.setName(processDefinitionName);

			ctx.getGraphSession().deployProcessDefinition(pd);

			try {

				@SuppressWarnings("unchecked")
				Collection<Task> tasks = pd.getTaskMgmtDefinition().getTasks()
						.values();

				for (Task task : tasks) {

					List<ViewResource> viewResources = processBundle
							.getViewResources(task.getName());
					
					if(viewResources != null) {
					
						for (ViewResource viewResource : viewResources) {

							View view = viewResource.store();
							getViewToTaskBinder().bind(view, task);
						}
					} else {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, "No view resources resolved for task: "+task.getId());
					}
				}
				
				ManagersTypeProcessDefinitionBind mtpdb = new ManagersTypeProcessDefinitionBind();
				mtpdb.setManagersType(managersType);
				mtpdb.setProcessDefinitionId(pd.getId());
				getBpmBindsDAO().persist(mtpdb);
				
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

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

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
}