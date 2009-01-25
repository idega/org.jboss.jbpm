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

		bind(view.getViewId(), view.getViewType(), task);
	}

	public void bind(String viewId, String viewType, Task task) {
		// TODO: catch when duplicate view type and task id pair is tried to be
		// entered, and override
		// views could be versioned

		ViewTaskBind vtb = getBPMDAO().getViewTaskBind(task.getId(), viewType);

		boolean newVtb = false;

		if (vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}

		vtb.setTaskId(task.getId());
		vtb.setTaskInstanceId(null);
		vtb.setViewIdentifier(viewId);
		vtb.setViewType(viewType);

		if (newVtb)
			getBPMDAO().persist(vtb);

	}

	public void bind(View view, TaskInstance taskInstance) {

		ViewTaskBind vtb = getBPMDAO().getViewTaskBindByTaskInstance(
				taskInstance.getId(), view.getViewType());

		boolean newVtb = false;

		if (vtb == null) {
			vtb = new ViewTaskBind();
			newVtb = true;
		}

		vtb.setTaskInstanceId(taskInstance.getId());
		vtb.setTaskId(null);
		vtb.setViewIdentifier(view.getViewId());
		vtb.setViewType(view.getViewType());

		if (newVtb)
			getBPMDAO().persist(vtb);
	}

	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}

	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}
}