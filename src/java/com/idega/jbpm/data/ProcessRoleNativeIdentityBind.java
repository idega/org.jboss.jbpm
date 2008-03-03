package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/03 12:34:57 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROLE_NIDENTITY")
@NamedQueries(
		{
		}
)
public class ProcessRoleNativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;
	
	@Column(name="role_name", nullable=false)
	private String processRoleName;
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actor_id")
	private Long actorId;
	
    @OneToMany(mappedBy=NativeIdentityBind.processRoleNativeIdentityProp)
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