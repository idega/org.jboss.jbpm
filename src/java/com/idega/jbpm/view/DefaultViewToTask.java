package com.idega.jbpm.view;

import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;

public class DefaultViewToTask implements ViewToTask {

	private String viewType;
	
	@Autowired
	private BPMDAO BPMDAO; 
	
	
	public DefaultViewToTask(String viewType){
		this.viewType = viewType;
	}
	
	public void bind(View view, Task task) {
//		TODO: view type and task id should be a alternate key. that means unique too.
//		also catch when duplicate view type and task id pair is tried to be entered, and override
//		views could be versioned
		
		ViewTaskBind vtb = getBPMDAO().getViewTaskBind(task.getId(), viewType);
		
		boolean newVtb = false;
		
		if(vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}
		
		vtb.setTaskId(task.getId());
		vtb.setTaskInstanceId(null);
		vtb.setViewIdentifier(view.getViewId());
		vtb.setViewType(view.getViewType());

		if(newVtb)
			getBPMDAO().persist(vtb);
		
	}

	public void bind(View view, TaskInstance taskInstance) {

		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByTaskInstance(taskInstance.getId(), viewType);
		
		boolean newVtb = false;
		
		if(vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}
		
		vtb.setTaskInstanceId(taskInstance.getId());
		vtb.setTaskId(null);
		vtb.setViewIdentifier(view.getViewId());
		vtb.setViewType(view.getViewType());

		if(newVtb)
			getBPMDAO().persist(vtb);
	
		
	}

	public Long getTask(String viewId) {
		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByView(viewId, viewType);
		return vtb == null ? null : vtb.getTaskId();
	}

	public void unbind(String viewId) {
		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByView(viewId, viewType);
		
		if(vtb != null)
			getBPMDAO().remove(vtb);
		
	}
	

	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}

	
	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}
	
}
