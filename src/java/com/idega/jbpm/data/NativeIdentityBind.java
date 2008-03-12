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
 * @version $Revision: 1.8 $
 *
 * Last modified: $Date: 2008/03/12 12:41:57 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_NATIVE_IDENTITY")
@NamedQueries(
		{
			@NamedQuery(name=NativeIdentityBind.deleteByIds, query="delete from NativeIdentityBind b where b.id in (:"+NativeIdentityBind.idsParam+")"),
			@NamedQuery(name=NativeIdentityBind.getByProcIdentity, query="from NativeIdentityBind b where b."+NativeIdentityBind.processRoleProperty+"."+ProcessRole.actorIdProperty+" = :"+NativeIdentityBind.procIdentityParam),
			@NamedQuery(name=NativeIdentityBind.getByTypesAndProceIdentities, query="select ni from NativeIdentityBind ni, com.idega.jbpm.data.ProcessRole prni where ni."+NativeIdentityBind.processRoleProperty+" = prni and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty+" and prni."+ProcessRole.actorIdProperty+" in (:"+ProcessRole.actorIdProperty+")")
		}
)
public class NativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;

	public static final String procIdentityParam = "procIdentity";
	
	public static final String idsParam = "ids";
	public static final String deleteByIds = "NativeIdentityBind.deleteByIds";
	public static final String getByProcIdentity = "NativeIdentityBind.getByProcIdentity";
	public static final String getByTypesAndProceIdentities = "NativeIdentityBind.getByTypesAndProceIdentities";
	
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

	public static final String identityTypeProperty = "identityType";
	@Column(name="identity_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private IdentityType identityType;

	public static final String processRoleProperty = "processRole";
	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinColumn(name="process_role_fk", nullable=false)
	private ProcessRole processRole;

	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
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

	public ProcessRole getProcessRole() {
		return processRole;
	}

	public void setProcessRole(ProcessRole processRole) {
		this.processRole = processRole;
	}

	public static String getProcessRoleNativeIdentityProp() {
		return processRoleProperty;
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