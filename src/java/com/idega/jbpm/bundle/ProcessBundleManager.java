package com.idega.jbpm.bundle;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.def.TaskMgmtDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.view.ViewResource;
import com.idega.jbpm.view.ViewToTask;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.18 $ Last modified: $Date: 2009/02/26 08:55:03 $ by $Author: civilis $
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
					
					if (tasksMap != null) {
						
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
									getViewToTask().bind(
									    viewResource.getViewId(),
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
					
					processBundle.configure(pd);
					
					return pd.getId();
					
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (Exception e) {
					
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
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