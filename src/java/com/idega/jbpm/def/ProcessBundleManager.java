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
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2008/01/27 13:11:36 $ by $Author: civilis $
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

	public long createBundle(ProcessBundle processBundle,
			String processDefinitionName) throws IOException, Exception {

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

				// specific
				// CasesJbpmBind bind = new CasesJbpmBind();
				// bind.setCasesCategoryId(caseCategoryId);
				// bind.setCasesTypeId(caseTypeId);
				// bind.setProcDefId(pd.getId());
				// bind.setInitTaskName(initTaskName);

				// getCasesJbpmDao().persist(bind);
				// -

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