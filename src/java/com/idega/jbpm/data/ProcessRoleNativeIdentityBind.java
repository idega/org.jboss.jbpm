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
 * @version $Revision: 1.6 $
 *
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROLE_NIDENTITY")
@NamedQueries(
		{
			@NamedQuery(name=ProcessRoleNativeIdentityBind.getAll, query="from ProcessRoleNativeIdentityBind"),
			@NamedQuery(name=ProcessRoleNativeIdentityBind.getAllByRoleNames, query="from ProcessRoleNativeIdentityBind b where b."+ProcessRoleNativeIdentityBind.processRoleNameProperty+" in(:"+ProcessRoleNativeIdentityBind.processRoleNameProperty+")")
		}
)
public class ProcessRoleNativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;
	
	public static final String getAll = "ProcessRoleNativeIdentityBind.getAll";
	public static final String getAllByRoleNames = "ProcessRoleNativeIdentityBind.getAllByRoleNames";
	public static final String actorIdProperty = "actorId";
	public static final String processRoleNameProperty = "processRoleName";
	
	@Column(name="role_name", nullable=false, unique=true)
	private String processRoleName;
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actor_id", nullable=false)
	private Long actorId;
	
    @OneToMany(mappedBy=NativeIdentityBind.processRoleNativeIdentityProp, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
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

	public void setActorId(Long actorId) {
		this.actorId = actorId;
	}

	public List<NativeIdentityBind> getNativeIdentities() {
		return nativeIdentities;
	}

	public void setNativeIdentities(List<NativeIdentityBind> nativeIdentities) {
		this.nativeIdentities = nativeIdentities;
	}
}