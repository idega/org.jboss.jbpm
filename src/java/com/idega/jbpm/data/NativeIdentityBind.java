package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/03/05 21:11:51 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_NATIVE_IDENTITY")
@NamedQueries(
		{
			@NamedQuery(name=NativeIdentityBind.deleteByIds, query="delete from NativeIdentityBind b where b.id in (:"+NativeIdentityBind.idsParam+")"),
			@NamedQuery(name=NativeIdentityBind.getByProcIdentity, query="from NativeIdentityBind b where b.processRoleNativeIdentity."+ProcessRoleNativeIdentityBind.actorIdProperty+" = :"+NativeIdentityBind.procIdentityParam)
		}
)
public class NativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;

	public static final String procIdentityParam = "procIdentity";
	public static final String idsParam = "ids";
	public static final String deleteByIds = "NativeIdentityBind.deleteByIds";
	public static final String getByProcIdentity = "NativeIdentityBind.getByProcIdentity";
	public static final String processRoleNativeIdentityProp = "processRoleNativeIdentity";

	public enum IdentityType {
		
		USER,
		GROUP,
		ROLE
	}
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id_")
	private Long id;

	@Column(name="identity_id", nullable=false)
	private String identityId;

	@Column(name="identity_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private IdentityType identityType;

	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinColumn(name="prole_nidentity_fk", nullable=false)
	private ProcessRoleNativeIdentityBind processRoleNativeIdentity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getIdentityId() {
		return identityId;
	}

	public void setIdentityId(String identityId) {
		this.identityId = identityId;
	}

	public IdentityType getIdentityType() {
		return identityType;
	}

	public void setIdentityType(IdentityType identityType) {
		this.identityType = identityType;
	}

	public ProcessRoleNativeIdentityBind getProcessRoleNativeIdentity() {
		return processRoleNativeIdentity;
	}

	public void setProcessRoleNativeIdentity(
			ProcessRoleNativeIdentityBind processRoleNativeIdentity) {
		this.processRoleNativeIdentity = processRoleNativeIdentity;
	}

	public static String getProcessRoleNativeIdentityProp() {
		return processRoleNativeIdentityProp;
	}
	
	@Override
	public boolean equals(Object arg0) {
		
		if(super.equals(arg0))
			return true;
		
		if(arg0 == null || !(arg0 instanceof NativeIdentityBind))
			return false;
		
		NativeIdentityBind b = (NativeIdentityBind)arg0;
		
		return getIdentityId() == null || getIdentityType() == null ? false : getIdentityId().equals(b.getIdentityId()) && getIdentityType().equals(b.getIdentityType());
	}
}