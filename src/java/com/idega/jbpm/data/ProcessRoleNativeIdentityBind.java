package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.idega.jbpm.identity.permission.Access;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/03/08 14:53:42 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROLE_NIDENTITY")
@NamedQueries(
		{
			@NamedQuery(name=ProcessRoleNativeIdentityBind.getAll, query="from ProcessRoleNativeIdentityBind"),
			@NamedQuery(name=ProcessRoleNativeIdentityBind.getAllByRoleNames, query="from ProcessRoleNativeIdentityBind b where b."+ProcessRoleNativeIdentityBind.processRoleNameProperty+" in(:"+ProcessRoleNativeIdentityBind.processRoleNameProperty+")"),
			@NamedQuery(name=ProcessRoleNativeIdentityBind.getAllByActorIds, query="from ProcessRoleNativeIdentityBind b where b."+ProcessRoleNativeIdentityBind.actorIdProperty+" in(:"+ProcessRoleNativeIdentityBind.actorIdProperty+")")
		}
)
public class ProcessRoleNativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;
	
	public static final String getAll = "ProcessRoleNativeIdentityBind.getAll";
	public static final String getAllByRoleNames = "ProcessRoleNativeIdentityBind.getAllByRoleNames";
	public static final String getAllByActorIds = "ProcessRoleNativeIdentityBind.getAllByActorIds";
	
	public static final String processRoleNameProperty = "processRoleName";
	@Column(name="role_name", nullable=false, unique=true)
	private String processRoleName;
	
	public static final String actorIdProperty = "actorId";
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actor_id", nullable=false)
	private Long actorId;
	
	@Column(name="has_read_access")
	private Boolean hasReadAccess;
	
	@Column(name="has_write_access")
	private Boolean hasWriteAccess;
	
//	TODO: add possibility to specify accesses for specific process definition. Those accesses would be override general accesses contained in this entity.
	
    @OneToMany(mappedBy=NativeIdentityBind.processRoleNativeIdentityProperty, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
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

	protected Boolean getHasReadAccess() {
		return hasReadAccess == null ? false : hasReadAccess;
	}

	protected void setHasReadAccess(Boolean hasReadAccess) {
		this.hasReadAccess = hasReadAccess;
	}

	protected Boolean getHasWriteAccess() {
		return hasWriteAccess == null ? false : hasWriteAccess;
	}

	protected void setHasWriteAccess(Boolean hasWriteAccess) {
		this.hasWriteAccess = hasWriteAccess;
	}
	
	public boolean hasAccess(Access access) {
		
		switch (access) {
		
		case read:
			return getHasReadAccess();

		case write:
			return getHasWriteAccess();
		default:
			return false;
		}
	}
	
	public void addAccess(Access access) {
		
		modifyAccess(access, true);
	}
	
	public void removeAccess(Access access) {
		
		modifyAccess(access, false);
	}
	
	protected void modifyAccess(Access access, boolean modifier) {
		
		switch (access) {
		
		case read:
			setHasReadAccess(modifier);

		case write:
			setHasWriteAccess(modifier);
		default:
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "access not recognized, skipping. Access got: "+access);
		}
	}
}