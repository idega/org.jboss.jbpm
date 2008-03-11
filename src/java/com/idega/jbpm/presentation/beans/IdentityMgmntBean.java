package com.idega.jbpm.presentation.beans;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.ProcessRole;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.user.business.GroupBusiness;
import com.idega.user.data.Group;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.webface.WFUtil;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/03/11 12:16:59 $ by $Author: civilis $
 */
public class IdentityMgmntBean implements Serializable {
	
	private static final long serialVersionUID = 2948950108387833369L;
	private Long selectedRoleActorId;
	private List<String> selectedNativeGroupIds;
	private String newRoleName;
	private List<SelectItem> rolesItems;
	private List<SelectItem> groupsItems;
	
	public BPMDAO getBpmBindsDAO() {
		return (BPMDAO)WFUtil.getBeanInstance("bpmBindsDAO");
	}

	public String getNewRoleName() {
		return newRoleName;
	}

	public void setNewRoleName(String newRoleName) {
		this.newRoleName = newRoleName;
	}

	public List<SelectItem> getRoles() {
		
		if(rolesItems == null) {
			
			rolesItems = new ArrayList<SelectItem>();
			
			List<ProcessRole> binds = getBpmBindsDAO().getAllProcessRoleNativeIdentityBinds();
			
			for (ProcessRole bind : binds) {
				
				SelectItem item = new SelectItem(bind.getActorId(), bind.getProcessRoleName());
				rolesItems.add(item);
			}
		}
		
		return rolesItems;
	}
	
	public List<SelectItem> getNativeGroups() {
		
		if(groupsItems == null) {
			
			groupsItems = new ArrayList<SelectItem>();
			
			try {
				@SuppressWarnings("unchecked")
				Collection<Group> groups = getGroupBusiness().getAllGroups();
				
				for (Group group : groups) {
					
					SelectItem item = new SelectItem(group.getPrimaryKey().toString(), group.getName());
					groupsItems.add(item);
				}
				
			} catch(RemoteException re) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving all groups", re);
			}
		}
		
		return groupsItems;
	}

	public void addGrpsToRole() {
		
		Long roleActorId = getSelectedRoleActorId();
		getBpmBindsDAO().updateAddGrpsToRole(roleActorId, getSelectedNativeGroupIds());
		
		if(selectedNativeGroupIds != null)
			selectedNativeGroupIds.clear();
		///selectedNativeGroupIds = null;
	}
	
	public void createProcessRole() {
		
		if(getNewRoleName() == null || CoreConstants.EMPTY.equals(getNewRoleName())) {
			
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Provide process role name", null);
			FacesContext.getCurrentInstance().addMessage(null, msg);
			return;
		}
		
		ProcessRole bind = new ProcessRole();
		bind.setProcessRoleName(getNewRoleName());
		
		getBpmBindsDAO().persist(bind);
		rolesItems = null;
		
		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Process role name saveds", null);
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public Long getSelectedRoleActorId() {
		return selectedRoleActorId;
	}

	public void setSelectedRoleActorId(Long selectedRoleActorId) {
		this.selectedRoleActorId = selectedRoleActorId;
	}
	
	public void selectedRoleActorIdChanged(ValueChangeEvent event) {
		
		PhaseId phaseId = event.getPhaseId();
		
		if (phaseId.equals(PhaseId.ANY_PHASE)) {
			
			event.setPhaseId(PhaseId.UPDATE_MODEL_VALUES);
			event.queue();
			
		} else if (phaseId.equals(PhaseId.UPDATE_MODEL_VALUES)) {
			
			System.out.println("event.getNewValue(): "+event.getNewValue());
			System.out.println("event.getOldValue(): "+event.getOldValue());
			if(selectedNativeGroupIds != null && event.getNewValue() != null && event.getOldValue() != null && !event.getNewValue().equals(event.getOldValue()))
				selectedNativeGroupIds.clear();
			
			selectedRoleActorId = (Long)event.getNewValue();
		}
	}

	public List<String> getSelectedNativeGroupIds() {
		
		if(selectedNativeGroupIds == null)
			selectedNativeGroupIds = new ArrayList<String>();
		
		if(selectedNativeGroupIds.isEmpty() && getSelectedRoleActorId() != null) {
			
			List<NativeIdentityBind> binds = getBpmBindsDAO().getNativeIdentities(getSelectedRoleActorId());
			
			for (NativeIdentityBind bind : binds)
				selectedNativeGroupIds.add(bind.getIdentityId());
		}
		
		return selectedNativeGroupIds;
	}

	public void setSelectedNativeGroupIds(List<String> selectedNativeGroupIds) {
		this.selectedNativeGroupIds = selectedNativeGroupIds;
	}

	protected GroupBusiness getGroupBusiness() {
		try {
			return (GroupBusiness)IBOLookup.getServiceInstance(CoreUtil.getIWContext(), GroupBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	/*
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
*/
}