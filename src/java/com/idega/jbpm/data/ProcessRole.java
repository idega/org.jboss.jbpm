package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 *
 * Last modified: $Date: 2008/04/26 02:48:33 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROCESS_ROLES")
@NamedQueries(
		{
			@NamedQuery(name=ProcessRole.getSetByRoleNamesAndProcessNameAndPIIdIsNull, query="from ProcessRole pr where pr."+ProcessRole.processNameProperty+" = :"+ProcessRole.processNameProperty+" and pr."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and pr."+ProcessRole.processInstanceIdProperty+" is null"),
			@NamedQuery(name=ProcessRole.getSetByRoleNamesAndPIId, query="from ProcessRole b where b."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and b."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty),
			@NamedQuery(name=ProcessRole.getAllByRoleNamesAndPIIdIsNull, query="from ProcessRole b where b."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and b."+ProcessRole.processInstanceIdProperty+" is null"),
			@NamedQuery(name=ProcessRole.getAllByActorIds, query="from ProcessRole b where b."+ProcessRole.actorIdProperty+" in(:"+ProcessRole.actorIdProperty+")")
		}
)
public class ProcessRole implements Serializable {

	private static final long serialVersionUID = -1167182554959904075L;
	public static final String getAllGeneral = "ProcessRoleNativeIdentityBind.getAllGeneral";
	public static final String getSetByRoleNamesAndPIId = "ProcessRoleNativeIdentityBind.getSetByRoleNamesAndPIId";
	public static final String getAllByRoleNamesAndPIIdIsNull = "ProcessRoleNativeIdentityBind.getAllByRoleNamesAndPIIdIsNull";
	public static final String getSetByRoleNamesAndProcessNameAndPIIdIsNull = "ProcessRoleNativeIdentityBind.getSetByRoleNamesAndProcessNameAndPIIdIsNull";
	
	
	public static final String getAllByActorIds = "ProcessRoleNativeIdentityBind.getAllByActorIds";
	
	public static final String processRoleNameProperty = "processRoleName";
	@Column(name="role_name", nullable=false)
	private String processRoleName;
	
	public static final String actorIdProperty = "actorId";
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actor_id")
	private Long actorId;
	
	public static final String processInstanceIdProperty = "processInstanceId";
    @Column(name="process_instance_id")
	private Long processInstanceId;
    
    public static final String processNameProperty = "processName";
    @Column(name="process_name")
	private String processName;
    
    @OneToMany(mappedBy=NativeIdentityBind.processRoleProperty, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	private List<NativeIdentityBind> nativeIdentities;
    
    @OneToMany(mappedBy=ActorPermissions.processRoleProperty, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	private List<ActorPermissions> actorPermissions;
    
	public String getProcessRoleName() {
		return processRoleName;
	}

	public void setProcessRoleName(String processRoleName) {
		this.processRoleName = processRoleName;
	}

	public Long getActorId() {
		return actorId;
	}

	protected void setActorId(Long actorId) {
		this.actorId = actorId;
	}

	public List<NativeIdentityBind> getNativeIdentities() {
		return nativeIdentities;
	}

	public void setNativeIdentities(List<NativeIdentityBind> nativeIdentities) {
		this.nativeIdentities = nativeIdentities;
	}

	public Long getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(Long processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public List<ActorPermissions> getActorPermissions() {
		return actorPermissions;
	}

	public void setActorPermissions(List<ActorPermissions> actorPermissions) {
		this.actorPermissions = actorPermissions;
	}
}