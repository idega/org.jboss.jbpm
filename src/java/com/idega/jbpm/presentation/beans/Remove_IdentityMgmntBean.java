package com.idega.jbpm.presentation.beans;

import java.io.Serializable;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/05 16:21:54 $ by $Author: laddi $
 */
public class Remove_IdentityMgmntBean implements Serializable {
	
	/*
	private static final long serialVersionUID = 2948950108387833369L;
	private Long selectedRoleActorId;
	private List<String> selectedNativeGroupIds;
	private String newRoleName;
	private List<SelectItem> rolesItems;
	private List<SelectItem> groupsItems;
	
	public BPMDAO getBpmBindsDAO() {
		return (BPMDAO)WFUtil.getBeanInstance("bpmBindsDAO");
	}
	
	public RolesManager getRolesManager() {
		return (RolesManager)WFUtil.getBeanInstance("bpmRolesManager");
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
			
			List<ProcessRole> genRoles = getRolesManager().getGeneralRoles();
			
			for (ProcessRole bind : genRoles) {
				
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
		
		if(getSelectedRoleActorId() != null && !getSelectedNativeGroupIds().isEmpty())
			getBpmBindsDAO().updateAddGrpsToRole(getSelectedRoleActorId(), getSelectedNativeGroupIds());
		
		if(selectedNativeGroupIds != null)
			selectedNativeGroupIds.clear();
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
	*/
}