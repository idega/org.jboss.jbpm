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
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/03 12:43:18 $ by $Author: alexis $
 */
@Entity
@Table(name="BPM_NATIVE_IDENTITY")
@NamedQueries(
		{
		}
)
public class NativeIdentityBind implements Serializable {

	private static final long serialVersionUID = 4739344819567695492L;
	
	public static final String processRoleNativeIdentityProp = "processRoleNativeIdentity";

	public enum IdentityType {
		
		USER,
		GROUP,
		ROLE
	}
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="id")
	private Long id;

	@Column(name="identity_id", nullable=false)
	private String identityId;

	@Column(name="identity_type", nullable=false)
	@Enumerated(EnumType.STRING)
	private IdentityType identityType;

	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="prole_nidentity_fk")
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
}