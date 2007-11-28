package com.idega.jbpm.presentation.beans;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.faces.model.SelectItem;

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.data.ICRole;
import com.idega.jbpm.business.JbpmProcessBusinessBean;
import com.idega.jbpm.data.ActorTaskBind;
import com.idega.jbpm.def.ActorTaskBinder;
import com.idega.user.business.GroupBusiness;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;

public class ActorBindingViewBean implements Serializable {
	
	private String actorType;
	private String[] actorId;
	private List actors = new ArrayList();
	private List<SelectItem> actorTypes = new ArrayList<SelectItem>();
	private boolean actorValueEdit = false;
	private String[] taskId;
	private List processes = new ArrayList();
	private String processId;

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}
	
	public String getActorLabel() {
		if(actorType == null || actorType.equals("")) {
			if(taskId != null) {
				Task task = getJbpmProcessBusiness().getProcessTask(new Long(processId), taskId[0]);
				if(task != null) {
					ActorTaskBind atb = getActorToTaskBinder().getActor(task.getId());
					if(atb != null) {
						this.actorType = atb.getActorType();
						this.actorId[0] = atb.getActorId();
					}
				}
				
			}
		}
		return actorType == null ? "" : "(" + actorType + ") " + actorToTaskBinder.getActorName(actorId[0], actorType);
	}

	public List getTasks() {
		return getJbpmProcessBusiness().getProcessDefinitionTasks(new Long(processId), false);
	}
	
	public List getTaskList(String processId) {
		this.processId = processId;
		return getJbpmProcessBusiness().getProcessDefinitionTasks(new Long(processId), true);
	}
	
	public List getProcesses() {
		processes.clear();
		processes.add(new SelectItem("", "--Select a process--"));
		List<ProcessDefinition> pdList = getJbpmProcessBusiness().getProcessList();
		for(Iterator<ProcessDefinition> it = pdList.iterator(); it.hasNext(); ) {
			ProcessDefinition pd = (ProcessDefinition) it.next();
			processes.add(new SelectItem(new Long(pd.getId()).toString(), pd.getName()));
		}
		return processes;
	}

	public String[] getTaskId() {
		return taskId;
	}

	public void setTaskId(String[] taskId) {
		this.taskId = taskId;
	}

	public boolean isActorValueEdit() {
		return actorValueEdit;
	}

	public void setActorValueEdit(boolean actorValueEdit) {
		this.actorValueEdit = actorValueEdit;
	}
	
	public String getActorType() {
		return actorType;
	}

	public void setActorType(String actorType) {
		this.actorType = actorType;
	}

	public List<SelectItem> getActorTypes() {
		actorTypes.clear();
		actorTypes.add(new SelectItem("", "----Actor types----"));
		actorTypes.add(new SelectItem(ActorTaskBind.USER, "User"));
		actorTypes.add(new SelectItem(ActorTaskBind.GROUP, "Group"));
		actorTypes.add(new SelectItem(ActorTaskBind.ROLE, "Role"));
		return actorTypes;
	}
	
	public List getActorsByType(String actorType, boolean forDWR) {
		List actors = new ArrayList();
		if(actorType != null) {
			this.actorType = actorType;
			if(actorType.equals(ActorTaskBind.USER)) {
				try {
					Collection users = getUserBusiness().getAllUsersOrderedByFirstName();
					for(Iterator it = users.iterator(); it.hasNext(); ) {
						User user = (User) it.next();
						if(forDWR) {
							AdvancedProperty roleItem = new AdvancedProperty(user.getId(), user.getName());
							actors.add(roleItem);
						} else {
							SelectItem roleItem = new SelectItem(user.getId(), user.getName());
							actors.add(roleItem);
						}
					}
				} catch(RemoteException re) {
					re.printStackTrace();
				} catch(FinderException fe) {
					fe.printStackTrace();
				}
			} else if(actorType.equals(ActorTaskBind.GROUP)) {
				try {
					Collection groups = getGroupBusiness().getAllGroups();
					for(Iterator it = groups.iterator(); it.hasNext(); ) {
						Group group = (Group) it.next();
						if(forDWR) {
							AdvancedProperty roleItem = new AdvancedProperty(group.getPrimaryKey().toString(), group.getName());
							actors.add(roleItem);
						} else {
							SelectItem roleItem = new SelectItem(group.getPrimaryKey().toString(), group.getName());
							actors.add(roleItem);
						}
					}
				} catch(RemoteException re) {
					re.printStackTrace();
				}
			} else if(actorType.equals(ActorTaskBind.ROLE)) {
				AccessController accessController = CoreUtil.getIWContext().getAccessController();
				Collection roles = accessController.getAllRoles();
				for(Iterator it = roles.iterator(); it.hasNext(); ) {
					ICRole role = (ICRole) it.next();
					if(forDWR) {
						AdvancedProperty roleItem = new AdvancedProperty(role.getId(), role.getRoleKey());
						actors.add(roleItem);
					} else {
						SelectItem roleItem = new SelectItem(role.getId(), role.getRoleKey());
						actors.add(roleItem);
					}
				}
			}
		}
		if(!forDWR) {
			this.actors = actors;
		}
		return actors;
	}
	
	public void saveActor() {
		if(actorType != null) {
			actorToTaskBinder.bindActor(processId, taskId[0], actorId[0], actorType);
			this.actorValueEdit = false;
			this.actorType = "";
		}
	}
	
	public void changeActor() {
		this.actorValueEdit = true;
	}
	
	public void cancelActor() {
		this.actorValueEdit = false;
	}

	public List getActors() {
		return getActorsByType(actorType, false);
	}

	public void setActors(List actors) {
		this.actors = actors;
	}

	public String[] getActorId() {
		return actorId;
	}

	public void setActorId(String[] actorId) {
		this.actorId = actorId;
	}

	protected UserBusiness getUserBusiness() {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	protected GroupBusiness getGroupBusiness() {
		try {
			return (GroupBusiness) IBOLookup.getServiceInstance(CoreUtil.getIWContext(), GroupBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	private ActorTaskBinder actorToTaskBinder;
	private JbpmProcessBusinessBean jbpmProcessBusiness;

	public JbpmProcessBusinessBean getJbpmProcessBusiness() {
		return jbpmProcessBusiness;
	}

	public void setJbpmProcessBusiness(JbpmProcessBusinessBean jbpmProcessBusiness) {
		this.jbpmProcessBusiness = jbpmProcessBusiness;
	}

	public ActorTaskBinder getActorToTaskBinder() {
		return actorToTaskBinder;
	}

	public void setActorToTaskBinder(ActorTaskBinder actorToTaskBinder) {
		this.actorToTaskBinder = actorToTaskBinder;
	}

}
