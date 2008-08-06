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
 * Process role represents the role specified in process definition. 
 * Actor id is the generated identifier, and used in task assignments.
 * Role name might be assigned to IC_PERMIT_ROLE table.
 * 
 * If there are no permissions for actor with process instance id != null, then the permissions for process name are taken (i.e. the permissions are specified for process definition scope). 
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.15 $
 *
 * Last modified: $Date: 2008/08/06 10:47:22 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_PROCESS_ROLES")
@NamedQueries(
		{
			//@NamedQuery(name=ProcessRole.getSetByRoleNamesAndProcessNameAndPIIdIsNull, query="from ProcessRole pr where pr."+ProcessRole.processNameProperty+" = :"+ProcessRole.processNameProperty+" and pr."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and pr."+ProcessRole.processInstanceIdProperty+" is null"),
			@NamedQuery(name=ProcessRole.getSetByRoleNamesAndPIId, query="from ProcessRole b where b."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and b."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty),
			//@NamedQuery(name=ProcessRole.getAllByRoleNamesAndPIIdIsNull, query="from ProcessRole b where b."+ProcessRole.processRoleNameProperty+" in(:"+ProcessRole.processRoleNameProperty+") and b."+ProcessRole.processInstanceIdProperty+" is null"),
			@NamedQuery(name=ProcessRole.getAllByActorIds, query="from ProcessRole b where b."+ProcessRole.actorIdProperty+" in(:"+ProcessRole.actorIdProperty+")"),
			@NamedQuery(name=ProcessRole.getAllProcessInstancesIds, query="select pr."+ProcessRole.processInstanceIdProperty+" from ProcessRole pr"),
			@NamedQuery(name=ProcessRole.getSetByPIId, query="from ProcessRole b where b."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty),
			@NamedQuery(name=ProcessRole.getSetByPIIds, query="from ProcessRole pr where pr."+ProcessRole.processInstanceIdProperty+" in(:"+ProcessRole.processInstanceIdProperty+")"),
			@NamedQuery(name=ProcessRole.getSetByPIIdsAndRoleNames, query="from ProcessRole pr where pr."+ProcessRole.processInstanceIdProperty+" in(:"+ProcessRole.processInstanceIdProperty+") and pr."+ProcessRole.processRoleNameProperty+" in (:"+ProcessRole.processRoleNameProperty+")"),
			
			
//			TODO verify that works with different databases (true thingy)
			@NamedQuery(name=ProcessRole.getRoleNameHavingRightsModifyPermissionByPIId, query=
				"select pr." + ProcessRole.processRoleNameProperty + " from ProcessRole pr, in (pr." + ProcessRole.actorPermissionsProperty + ") permissions where pr."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty + " and permissions." + ActorPermissions.modifyRightsPermissionProperty + " = true"),
		    @NamedQuery(name=ProcessRole.getRolesNamesHavingCaseHandlerRights, query=
					"select pr." + ProcessRole.processRoleNameProperty + " from ProcessRole pr inner join pr." + ProcessRole.actorPermissionsProperty + " permissions where pr."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty + " and permissions." + ActorPermissions.caseHandlerPermissionProperty + " = true"),
			@NamedQuery(name=ProcessRole.getRolesHavingCaseHandlerRights, query=
					"select pr from ProcessRole pr inner join pr." + ProcessRole.actorPermissionsProperty + " permissions where pr."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty + " and permissions." + ActorPermissions.caseHandlerPermissionProperty + " = true")
			
			/*, TODO: remove
			@NamedQuery(name=ProcessRole.getSetHavingPermissionsByTaskIdAndRoleName, query=
				//"select pr.roleName from ProcessRole pr inner join ActorPermission ap with ap.taskId = :taskId and pr.roleName in (:roleName)"
				"select pr from ProcessRole pr left join com.idega.jbpm.data.ActorPermissions ap with pr."+ProcessRole.processInstanceIdProperty+" = :"+ProcessRole.processInstanceIdProperty+" and pr."+ProcessRole.processRoleNameProperty+" in (:"+ProcessRole.processRoleNameProperty+") and ap."+ActorPermissions.taskIdProperty+" = :"+ActorPermissions.taskIdProperty
				
				//"from ProcessRole b where b."+ProcessRole.actorIdProperty+" in(:"+ProcessRole.actorIdProperty+")"
			)
			*/
			
/*		
 * 	@NamedQuery(name=ProcessRole.getProcessInstanceIdsByUserIdentity, 
					query=
						"select pr."+ProcessRole.processInstanceIdProperty+" from ProcessRole pr "+
						"inner join com.idega.jbpm.data.NativeIdentityBind ni "+ 
						"where ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+" and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty
			),
			@NamedQuery(name=ProcessRole.getProcessInstanceIdsByUserRolesAndUserIdentity, 
					query=
						"select pr."+ProcessRole.processInstanceIdProperty+" from ProcessRole pr "+
						"where pr."+ProcessRole.processRoleNameProperty+" in (:"+ProcessRole.processRoleNameProperty+") "+
						"union "+
						"select pr."+ProcessRole.processInstanceIdProperty+" from ProcessRole pr "+
						"inner join com.idega.jbpm.data.NativeIdentityBind ni "+ 
						"where ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+" and ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty
			)
			*/
		}
)
@SqlResultSetMapping(name="processInstanceId", columns=@ColumnResult(name="processInstanceId"))
@NamedNativeQueries(
		{
			@NamedNativeQuery(name=ProcessRole.getProcessInstanceIdsByUserIdentity, resultSetMapping="processInstanceId",
					query=
						"select pr.process_instance_id as processInstanceId from "+ProcessRole.TABLE_NAME+" pr "+
						"inner join "+NativeIdentityBind.TABLE_NAME+" ni "+ 
						"on ni.process_role_fk = pr.actor_id "+
						"where ni.identity_id = :"+NativeIdentityBind.identityIdProperty+" and ni.identity_type = :"+NativeIdentityBind.identityTypeProperty
			),
			@NamedNativeQuery(name=ProcessRole.getProcessInstanceIdsByUserRolesAndUserIdentity, resultSetMapping="processInstanceId",
					query=
						"select pr.process_instance_id as processInstanceId from "+ProcessRole.TABLE_NAME+" pr "+
						"where pr.role_name in (:"+ProcessRole.processRoleNameProperty+") and pr.process_instance_id is not null "+
						"union "+
						"select pr.process_instance_id as processInstanceId from "+ProcessRole.TABLE_NAME+" pr "+
						"inner join "+NativeIdentityBind.TABLE_NAME+" ni "+ 
						"on ni.process_role_fk = pr.actor_id "+
						"where ni.identity_id = :"+NativeIdentityBind.identityIdProperty+" and ni.identity_type = :"+NativeIdentityBind.identityTypeProperty
			)
		}
)
public class ProcessRole implements Serializable {

	private static final long serialVersionUID = -1167182554959904075L;
	
	public static final String TABLE_NAME = "BPM_PROCESS_ROLES";
	
	public static final String getAllGeneral = "ProcessRole.getAllGeneral";
	public static final String getSetByPIId = "ProcessRole.getSetByPIId";
	public static final String getSetByRoleNamesAndPIId = "ProcessRole.getSetByRoleNamesAndPIId";
	public static final String getSetByPIIds = "ProcessRole.getSetByPIIds";
	public static final String getSetByPIIdsAndRoleNames = "ProcessRole.getSetByPIIdsAndRoleNames";
//	public static final String getAllByRoleNamesAndPIIdIsNull = "ProcessRole.getAllByRoleNamesAndPIIdIsNull";
	///public static final String getSetByRoleNamesAndProcessNameAndPIIdIsNull = "ProcessRole.getSetByRoleNamesAndProcessNameAndPIIdIsNull";
	public static final String getProcessInstanceIdsByUserIdentity = "ProcessRole.getProcessInstanceIdsByUserIdentity";
	public static final String getProcessInstanceIdsByUserRolesAndUserIdentity = "ProcessRole.getProcessInstanceIdsByUserRolesAndUserIdentity";

	public static final String getAllByActorIds = "ProcessRole.getAllByActorIds";
	public static final String getAllProcessInstancesIds = "ProcessRole.getAllProcessInstancesIds";
	public static final String getSetHavingPermissionsByTaskIdAndRoleName = "ProcessRole.getSetRolesNamesHavingPermissionsByTaskIdAndRoleName";
	public static final String getRoleNameHavingRightsModifyPermissionByPIId = "ProcessRole.getHavingRightsModifyPermissionByPIId";
	public static final String getRolesNamesHavingCaseHandlerRights = "ProcessRole.getRolesNamesHavingCaseHandlerRights";
	public static final String getRolesHavingCaseHandlerRights = "ProcessRole.getRolesHavingCaseHandlerRights";
	
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
    
//    @OneToMany(mappedBy=ActorPermissions.processRoleProperty, cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
//	private List<ActorPermissions> actorPermissions;

    public static final String actorPermissionsProperty = "actorPermissions";
    @ManyToMany(
            targetEntity=ActorPermissions.class,
            cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH},
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name="BPM_ROLES_PERMISSIONS",
            joinColumns=@JoinColumn(name="ROLE_ACTOR_ID"),
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