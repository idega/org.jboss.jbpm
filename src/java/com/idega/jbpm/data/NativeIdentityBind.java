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
 * Used additionally to roles + ic_permit_role. In common use case, the identity type is user.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.12 $
 *
 * Last modified: $Date: 2008/08/25 19:02:33 $ by $Author: civilis $
 */
@Entity
@Table(name=NativeIdentityBind.TABLE_NAME)
@NamedQueries(
		{
			@NamedQuery(name=NativeIdentityBind.deleteByIds, query="delete from NativeIdentityBind b where b.id in (:"+NativeIdentityBind.idsParam+")"),
			@NamedQuery(name=NativeIdentityBind.getByProcIdentity, query="from NativeIdentityBind b where b."+NativeIdentityBind.actorProperty+"."+Actor.actorIdProperty+" = :"+NativeIdentityBind.procIdentityParam),
			@NamedQuery(name=NativeIdentityBind.getByTypesAndProceIdentities, query="select ni from NativeIdentityBind ni, com.idega.jbpm.data.Actor prni where ni."+NativeIdentityBind.actorProperty+" = prni and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty+" and prni."+Actor.actorIdProperty+" in (:"+Actor.actorIdProperty+")")
		}
)
public class NativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;

	public static final String TABLE_NAME = "BPM_NATIVE_IDENTITIES";
	public static final String procIdentityParam = "procIdentity";
	
	public static final String idsParam = "ids";
	public static final String deleteByIds = "NativeIdentityBind.deleteByIds";
	public static final String getByProcIdentity = "NativeIdentityBind.getByProcIdentity";
	public static final String getByTypesAndProceIdentities = "NativeIdentityBind.getByTypesAndProceIdentities";
	
	public enum IdentityType {
		
		USER,
		BPMUSER,
		GROUP,
		ROLE
	}
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id_")
	private Long id;

	public static final String identityIdProperty = "identityId";
	@Column(name="identity_id", nullable=false)
	private String identityId;

	public static final String identityTypeProperty = "identityType";
	@Column(name="identity_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private IdentityType identityType;

	public static final String actorProperty = "actor";
	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinColumn(name="actor_fk", nullable=false)
	private Actor actor;

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

	public Actor getActor() {
		return actor;
	}

	public void setActor(Actor actor) {
		this.actor = actor;
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