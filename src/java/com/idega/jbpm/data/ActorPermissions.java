package com.idega.jbpm.data;


import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Actor permissions for task or taskInstance. TaskInstance permissions should override ones specified for Task.
 * If variableName is not null, then for taskInstance or task, the most permissive permission should be used.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/06/26 15:33:33 $ by $Author: anton $
 */
@Entity
@Table(name="BPM_ACTORS_PERMISSIONS")
@NamedQueries(
		{
			//@NamedQuery(name=ActorPermissions.getSetByTaskIdAndProcessRole, query="from ActorPermissions ap where ap."+ActorPermissions.taskIdProperty+" = :"+ActorPermissions.taskIdProperty+" and "+ActorPermissions.processRoleProperty+" in (:"+ActorPermissions.processRoleProperty+")")
			@NamedQuery(name=ActorPermissions.getSetByTaskIdAndProcessRoleNames, query="from ActorPermissions ap where ap."+ActorPermissions.taskIdProperty+" = :"+ActorPermissions.taskIdProperty+" and "+ActorPermissions.roleNameProperty+" in (:"+ActorPermissions.roleNameProperty+")"),
			@NamedQuery(name=ActorPermissions.getSetByProcessRoleNames, query="from ActorPermissions ap where "+ActorPermissions.roleNameProperty+" in (:"+ActorPermissions.roleNameProperty+")"),
			@NamedQuery(name=ActorPermissions.getByProcessIdAndModifyRights, query="from ActorPermissions ap where "+ActorPermissions.roleNameProperty+" in (:"+ActorPermissions.roleNameProperty+")")
		}
)
public class ActorPermissions implements Serializable {

	private static final long serialVersionUID = 4768266953928292205L;
	
	//public static final String getSetByTaskIdAndProcessRole = "ActorPermissions.getSetByTaskIdAndProcessRole";
	public static final String getSetByTaskIdAndProcessRoleNames = "ActorPermissions.getSetByTaskIdAndProcessRoleNames";
	public static final String getSetByProcessRoleNames = "ActorPermissions.getSetByProcessRoleNames";
	public static final String getByProcessIdAndModifyRights = "ActorPermissions.getSetByProcessRoleNames";

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actperm_id")
	private Long id;
	
	public static final String taskIdProperty = "taskId";
	@Column(name="task_id")
	private Long taskId;
	
	public static final String taskInstanceIdProperty = "taskInstanceId";
	@Column(name="task_instance_id")
	private Long taskInstanceId;
	
	@Column(name="variable_identifier")
	private String variableIdentifier;
	
	public static final String roleNameProperty = "roleName";
	@Column(name="process_role_name")
	private String roleName;
	
	public static final String readPermissionProperty = "readPermission";
	@Column(name="has_read_permission", nullable=false)
	private Boolean readPermission;
	
	public static final String writePermissionProperty = "writePermission";
	@Column(name="has_write_permission", nullable=false)
	private Boolean writePermission;
	
	public static final String modifyRightsPermissionProperty = "modifyRightsPermission";
	@Column(name="can_modify_rights_permission")
	private Boolean modifyRightsPermission;
	
//	public static final String processRoleProperty = "processRole";
//	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
//    @JoinColumn(name="process_role_actor_id", nullable=false)
//	private ProcessRole processRole;
	
	@ManyToMany(
	        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
	        mappedBy = ProcessRole.actorPermissionsProperty,
	        targetEntity = ProcessRole.class
	)
	private List<ProcessRole> processRoles;
	
	public Long getId() {
		return id;
	}
	protected void setId(Long id) {
		this.id = id;
	}
	public Long getTaskId() {
		return taskId;
	}
	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}
	public Long getTaskInstanceId() {
		return taskInstanceId;
	}
	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
	public Boolean getReadPermission() {
		return readPermission;
	}
	public void setReadPermission(Boolean readPermission) {
		this.readPermission = readPermission;
	}
	public Boolean getWritePermission() {
		return writePermission;
	}
	public void setWritePermission(Boolean writePermission) {
		this.writePermission = writePermission;
	}
	public Boolean getModifyRightsPermission() {
		return modifyRightsPermission;
	}
	public void setModifyRightsPermission(Boolean modifyRightsPermission) {
		this.modifyRightsPermission = modifyRightsPermission;
	}
	public List<ProcessRole> getProcessRoles() {
		return processRoles;
	}
	public void setProcessRoles(List<ProcessRole> processRoles) {
		this.processRoles = processRoles;
	}
	public String getRoleName() {
		return roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	public String getVariableIdentifier() {
		return variableIdentifier;
	}
	public void setVariableIdentifier(String variableIdentifier) {
		this.variableIdentifier = variableIdentifier;
	}
}