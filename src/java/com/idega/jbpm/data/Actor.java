package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

/**
 * Actor represents the role specified in process definition, some user, or users group. 
 * Actor id is the generated identifier, and used in task assignments.
 * Role name might be assigned to IC_PERMIT_ROLE table.
 * 
 * If there are no permissions for actor with process instance id != null, then the permissions for process name are taken (i.e. the permissions are specified for process definition scope). 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2009/01/13 10:47:57 $ by $Author: anton $
 */
@Entity
@Table(name=Actor.TABLE_NAME)
@NamedQueries(
		{
			//@NamedQuery(name=Actor.getSetByRoleNamesAndProcessNameAndPIIdIsNull, query="from Actor pr where pr."+Actor.processNameProperty+" = :"+Actor.processNameProperty+" and pr."+Actor.processRoleNameProperty+" in(:"+Actor.processRoleNameProperty+") and pr."+Actor.processInstanceIdProperty+" is null"),
			@NamedQuery(name=Actor.getSetByRoleNamesAndPIId, query="from Actor b where b."+Actor.processRoleNameProperty+" in(:"+Actor.processRoleNameProperty+") and b."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty),
			@NamedQuery(name=Actor.getSetByPIIdsAndRoleNames, query="from Actor pr where pr."+Actor.processInstanceIdProperty+" in(:"+Actor.processInstanceIdProperty+") and pr."+Actor.processRoleNameProperty+" in (:"+Actor.processRoleNameProperty+")"),
			//@NamedQuery(name=Actor.getAllByRoleNamesAndPIIdIsNull, query="from Actor b where b."+Actor.processRoleNameProperty+" in(:"+Actor.processRoleNameProperty+") and b."+Actor.processInstanceIdProperty+" is null"),
			@NamedQuery(name=Actor.getAllByActorIds, query="from Actor b where b."+Actor.actorIdProperty+" in(:"+Actor.actorIdProperty+")"),
			@NamedQuery(name=Actor.getAllProcessInstancesIdsHavingRoleName, query="select distinct a."+Actor.processInstanceIdProperty+" from Actor a where a."+Actor.processRoleNameProperty+" is not null"),
			@NamedQuery(name=Actor.getSetByPIIdHavingRoleName, query="from Actor a where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+" and a."+Actor.processRoleNameProperty+" is not null"),
			@NamedQuery(name=Actor.getSetByPIIdAndNotContainingRoleNames, query="from Actor a where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+" and a."+Actor.processRoleNameProperty+" is not null and a."+Actor.processRoleNameProperty+" not in (:"+Actor.processRoleNameProperty+")"),
			@NamedQuery(name=Actor.getSetByPIIdsHavingRoleName, query="from Actor a where a."+Actor.processInstanceIdProperty+" in(:"+Actor.processInstanceIdProperty+") and a."+Actor.processRoleNameProperty+" is not null"),
			
//			TODO verify that works with different databases (true thingy)
			@NamedQuery(name=Actor.getRoleNameHavingRightsModifyPermissionByPIId, query=
				"select a." + Actor.processRoleNameProperty + " from Actor a, in (a." + Actor.actorPermissionsProperty + ") permissions where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty + " and permissions." + ActorPermissions.modifyRightsPermissionProperty + " = true and a."+Actor.processRoleNameProperty+" is not null"),
		    @NamedQuery(name=Actor.getRolesNamesHavingCaseHandlerRights, query=
				"select a." + Actor.processRoleNameProperty + " from Actor a inner join a." + Actor.actorPermissionsProperty + " permissions where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty + " and permissions." + ActorPermissions.caseHandlerPermissionProperty + " = true and a."+Actor.processRoleNameProperty+" is not null"),
			@NamedQuery(name=Actor.getRolesHavingCaseHandlerRights, query=
				"select a from Actor a inner join a." + Actor.actorPermissionsProperty + " permissions where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty + " and permissions." + ActorPermissions.caseHandlerPermissionProperty + " = true and a."+Actor.processRoleNameProperty+" is not null"),
			
			@NamedQuery(name=Actor.getProcessInstanceIdsByUserIdentity, query=
				"select act."+Actor.processInstanceIdProperty+" as piid from Actor act "+
				"inner join act."+Actor.nativeIdentitiesProperty+" ni "+ 
				"where ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+" and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty),
				
			@NamedQuery(name=Actor.getActorsByUserIdentityAndProcessInstanceId, query=
				"select act from Actor act "+
				"inner join act."+Actor.nativeIdentitiesProperty+" ni "+ 
				"where act."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+" and ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+" and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty),
				
			@NamedQuery(name=Actor.getTaskInstancesByUserRole, query="select task_instance.id from org.jbpm.taskmgmt.exe.TaskInstance task_instance where task_instance." + Actor.actorIdProperty + "= :" + Actor.actorIdProperty + " and task_instance." + Actor.createParam + "< :" + Actor.createParam + " order by task_instance." + Actor.createParam + " desc")
		}
)
@SqlResultSetMapping(name="processInstanceId", columns=@ColumnResult(name="processInstanceId"))
@NamedNativeQueries(
		{
			/*
			@NamedNativeQuery(name=Actor.getProcessInstanceIdsByUserIdentity, resultSetMapping="processInstanceId",
					query=
//						TODO: no need for native here, move to native identities and rewrite in jpaql
						"select pr.process_instance_id as processInstanceId from "+Actor.TABLE_NAME+" pr "+
						"inner join "+NativeIdentityBind.TABLE_NAME+" ni "+ 
						"on ni.process_role_fk = pr.actor_id "+
						"where ni.identity_id = :"+NativeIdentityBind.identityIdProperty+" and ni.identity_type = :"+NativeIdentityBind.identityTypeProperty
			),
			*/
			@NamedNativeQuery(name=Actor.getProcessInstanceIdsByUserRolesAndUserIdentity, resultSetMapping="processInstanceId",
					query=
//						using native, because jpql doesn't support union
						"select act.process_instance_id as processInstanceId from "+Actor.TABLE_NAME+" act "+
						"where act.role_name in (:"+Actor.processRoleNameProperty+") and act.process_instance_id is not null "+
						"union "+
						"select act.process_instance_id as processInstanceId from "+Actor.TABLE_NAME+" act "+
						"inner join "+NativeIdentityBind.TABLE_NAME+" ni "+ 
						"on ni.actor_fk = act.actor_id "+
						"where ni.identity_id = :"+NativeIdentityBind.identityIdProperty+" and ni.identity_type = :"+NativeIdentityBind.identityTypeProperty
			)
		}
)
public class Actor implements Serializable {

	private static final long serialVersionUID = -1167182554959904075L;
	
	public static final String TABLE_NAME = "BPM_ACTORS";
	
	public static final String getAllGeneral = "Actor.getAllGeneral";
	public static final String getSetByPIIdHavingRoleName = "Actor.getSetByPIIdHavingRoleName";
	public static final String getSetByPIIdAndNotContainingRoleNames = "Actor.getSetByPIIdAndNotContainingRoleNames";
	public static final String getSetByRoleNamesAndPIId = "Actor.getSetByRoleNamesAndPIId";
	public static final String getSetByPIIdsHavingRoleName = "Actor.getSetByPIIds";
	public static final String getSetByPIIdsAndRoleNames = "Actor.getSetByPIIdsAndRoleNames";
	public static final String getProcessInstanceIdsByUserIdentity = "Actor.getProcessInstanceIdsByUserIdentity";
	public static final String getProcessInstanceIdsByUserRolesAndUserIdentity = "Actor.getProcessInstanceIdsByUserRolesAndUserIdentity";
	public static final String getTaskInstancesByUserRole = "Actor.getTaskInstanceIdsByUserRole";
	public static final String getActorsByUserIdentityAndProcessInstanceId = "Actor.getActorsByUserIdentity";

	public static final String getAllByActorIds = "Actor.getAllByActorIds";
	public static final String getAllProcessInstancesIdsHavingRoleName = "Actor.getAllProcessInstancesIds";
	public static final String getSetHavingPermissionsByTaskIdAndRoleName = "Actor.getSetRolesNamesHavingPermissionsByTaskIdAndRoleName";
	public static final String getRoleNameHavingRightsModifyPermissionByPIId = "Actor.getHavingRightsModifyPermissionByPIId";
	public static final String getRolesNamesHavingCaseHandlerRights = "Actor.getRolesNamesHavingCaseHandlerRights";
	public static final String getRolesHavingCaseHandlerRights = "Actor.getRolesHavingCaseHandlerRights";
	
	public static final String createParam = "create";
	
	public static final String processRoleNameProperty = "processRoleName";
	@Column(name="role_name")
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
    
    public static final String nativeIdentitiesProperty = "nativeIdentities";
    @OneToMany(mappedBy=NativeIdentityBind.actorProperty, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
	private List<NativeIdentityBind> nativeIdentities;
    
    public static final String actorPermissionsProperty = "actorPermissions";
    @ManyToMany(
            targetEntity=ActorPermissions.class,
            cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name="BPM_ACT_PERMS",
            joinColumns=@JoinColumn(name="ACTOR_ID"),
            inverseJoinColumns=@JoinColumn(name="PERMISSION_ID")
    )
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