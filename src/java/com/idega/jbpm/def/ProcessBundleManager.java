package com.idega.jbpm.def;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

import com.idega.jbpm.IdegaJbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/01/30 14:33:51 $ by $Author: civilis $
 */
public class ProcessBundleManager {

	private ViewToTask viewToTaskBinder;
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

					for (ViewResource viewResource : viewResources) {

						View view = viewResource.store();
						getViewToTaskBinder().bind(view, task);
					}
				}
				
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

			ctx.close();
		}
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
}