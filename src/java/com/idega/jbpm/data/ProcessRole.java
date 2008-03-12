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
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/03/12 15:43:02 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROCESS_ROLES")
@NamedQueries(
		{
			@NamedQuery(name=ProcessRole.getAllGeneral, query="from ProcessRole pr where pr."+ProcessRole.processInstanceIdProperty+" is null"),
			@NamedQuery(name=ProcessRole.getAllByRoleNamesAndPIId, query="from ProcessRole b where b."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and b."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty),
			@NamedQuery(name=ProcessRole.getAllByRoleNamesAndPIIdIsNull, query="from ProcessRole b where b."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and b."+ProcessRole.processInstanceIdProperty+" is null"),
			@NamedQuery(name=ProcessRole.getAllByActorIds, query="from ProcessRole b where b."+ProcessRole.actorIdProperty+" in(:"+ProcessRole.actorIdProperty+")")
		}
)
public class ProcessRole implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;
	
	public static final String getAllGeneral = "ProcessRoleNativeIdentityBind.getAllGeneral";
	public static final String getAllByRoleNamesAndPIId = "ProcessRoleNativeIdentityBind.getAllByRoleNamesAndPIId";
	public static final String getAllByRoleNamesAndPIIdIsNull = "ProcessRoleNativeIdentityBind.getAllByRoleNamesAndPIIdIsNull";
	
	public static final String getAllByActorIds = "ProcessRoleNativeIdentityBind.getAllByActorIds";
	
	public static final String processRoleNameProperty = "processRoleName";
	@Column(name="role_name", nullable=false)
	private String processRoleName;
	
	public static final String actorIdProperty = "actorId";
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actor_id", nullable=false)
	private Long actorId;
	
	public static final String processInstanceIdProperty = "processInstanceId";
    @Column(name="process_instance_id")
	private Long processInstanceId;
	
    @OneToMany(mappedBy=NativeIdentityBind.processRoleProperty, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	private List<NativeIdentityBind> nativeIdentities;
    
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
}