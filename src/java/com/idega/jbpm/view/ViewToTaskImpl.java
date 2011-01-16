package com.idega.jbpm.view;

import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;

@Service
@Scope("singleton")
public class ViewToTaskImpl implements ViewToTask {

	@Autowired
	private BPMDAO BPMDAO;

	public void bind(View view, Task task) {
		bind(task.getId(), null, view.getViewId(), view.getViewType(), null);
	}

	public void bind(String viewId, String viewType, Task task, Integer order) {
		bind(task.getId(), null, viewId, viewType, order);
	}
	
	public void bind(View view, TaskInstance taskInstance) {
		bind(null, taskInstance.getId(), view.getViewId(), view.getViewType(), null);
	}
	
	private void bind(Long taskId, Long taskInstanceId, String viewId, String viewType, Integer order) {
		ViewTaskBind vtb = null;
		try {
			vtb = taskInstanceId == null ?	getBPMDAO().getViewTaskBind(taskId, viewType) :
											getBPMDAO().getViewTaskBindByTaskInstance(taskInstanceId, viewType);
		} catch (Exception e) {
			e.printStackTrace();
		}

		boolean newVtb = false;
		if (vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}

		vtb.setTaskId(taskId);
		vtb.setTaskInstanceId(taskInstanceId);
		vtb.setViewIdentifier(viewId);
		vtb.setViewType(viewType);
		vtb.setViewOrder(order);

		if (newVtb)
			getBPMDAO().persist(vtb);
		else
			getBPMDAO().merge(vtb);
	}
	
	public boolean containsBind(String viewType, Long taskInstanceId) {
		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByTaskInstance(taskInstanceId, viewType);
		return vtb != null;
	}

	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}

	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}
}