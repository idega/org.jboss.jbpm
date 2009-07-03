package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.ArrayList;
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
 * Actor permissions for task or taskInstance. TaskInstance permissions should override ones
 * specified for Task. If variableName is not null, then for taskInstance or task, the most
 * permissive permission should be used. Also used for contacts access management.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.16 $ Last modified: $Date: 2009/07/03 08:58:28 $ by $Author: valdas $
 */
@Entity
@Table(name = "BPM_ACTORS_PERMISSIONS")
@NamedQueries( {
        @NamedQuery(name = ActorPermissions.getSetByTaskIdAndProcessRoleNames, query = "from ActorPermissions ap where ap."
                + ActorPermissions.taskIdProperty
                + " = :"
                + ActorPermissions.taskIdProperty
                + " and "
                + ActorPermissions.roleNameProperty
                + " in (:"
                + ActorPermissions.roleNameProperty + ")"),
        @NamedQuery(name = ActorPermissions.getSetByTaskIdOrTaskInstanceId, query = "from ActorPermissions ap where ap."
                + ActorPermissions.taskIdProperty
                + " = :"
                + ActorPermissions.taskIdProperty
                + " or ap."
                + ActorPermissions.taskInstanceIdProperty
                + " = :"
                + ActorPermissions.taskInstanceIdProperty),
        @NamedQuery(name = ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdPureRoles, query = "from ActorPermissions ap inner join ap."
                + ActorPermissions.actorsProperty
                + " actors where ap."
                + ActorPermissions.roleNameProperty
                + " in (:"
                + ActorPermissions.roleNameProperty
                + ") and actors."
                + Actor.processInstanceIdProperty
                + " = :"
                + Actor.processInstanceIdProperty
                + " and ap."
                + ActorPermissions.taskIdProperty
                + " is null and ap."
                + ActorPermissions.taskInstanceIdProperty
                + " is null and ap."
                + ActorPermissions.canSeeContactsOfRoleNameProperty
                + " is null"),
        @NamedQuery(name = ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdForContacts, query = "from ActorPermissions ap inner join ap."
                + ActorPermissions.actorsProperty
                + " actors where ap."
                + ActorPermissions.roleNameProperty
                + " in (:"
                + ActorPermissions.roleNameProperty
                + ") and actors."
                + Actor.processInstanceIdProperty
                + " = :"
                + Actor.processInstanceIdProperty
                + " and ap."
                + ActorPermissions.taskIdProperty
                + " is null and ap."
                + ActorPermissions.taskInstanceIdProperty
                + " is null and ap."
                + ActorPermissions.canSeeContactsOfRoleNameProperty
                + " is not null"),
        @NamedQuery(name = ActorPermissions.getSetByProcessInstanceIdAndContactPermissionsRolesNames, query = "select ap from ActorPermissions ap inner join ap."
                + ActorPermissions.actorsProperty
                + " actors where actors."
                + Actor.processInstanceIdProperty
                + " = :"
                + Actor.processInstanceIdProperty
                + " and ap."
                + ActorPermissions.canSeeContactsOfRoleNameProperty
                + " in (:"
                + ActorPermissions.canSeeContactsOfRoleNameProperty + ")"),
        @NamedQuery(name = ActorPermissions.getSetByProcessInstanceIdAndCanSeeContacts, query = "select ap from ActorPermissions ap inner join ap."
                + ActorPermissions.actorsProperty
                + " act where act."
                + Actor.processInstanceIdProperty
                + " = :"
                + Actor.processInstanceIdProperty
                + " and ap."
                + ActorPermissions.canSeeContactsOfRoleNameProperty
                + " is not null"),
        @NamedQuery(name = ActorPermissions.getSetByProcessInstanceIdAndCanSeeComments, query = "select ap from ActorPermissions ap inner join ap."
                    + ActorPermissions.actorsProperty
                    + " act where act."
                    + Actor.processInstanceIdProperty
                    + " = :"
                    + Actor.processInstanceIdProperty
                    + " and ap."
                    + ActorPermissions.canSeeCommentsProperty
                    + " is not null"),
        @NamedQuery(name = ActorPermissions.getSetByProcessInstanceIdAndCanWriteComments, query = "select ap from ActorPermissions ap inner join ap."
                 + ActorPermissions.actorsProperty
                 + " act where act."
                 + Actor.processInstanceIdProperty
                 + " = :"
                 + Actor.processInstanceIdProperty
                 + " and ap."
                 + ActorPermissions.canWriteCommentsProperty
                 + " is not null"),
        @NamedQuery(name = ActorPermissions.getSetByProcessInstanceIdAndCanSeeAttachments, query = "select ap from ActorPermissions ap inner join ap."
                 + ActorPermissions.actorsProperty
                 + " act where act."
                 + Actor.processInstanceIdProperty
                 + " = :"
                 + Actor.processInstanceIdProperty
                 + " and ap."
                 + ActorPermissions.canSeeAttachmentsProperty
                 + " is not null"),
        @NamedQuery(name = ActorPermissions.getSetByTaskIdOrTaskInstanceIdAndVariableIdentifier, query = "from ActorPermissions ap where ap."
                 + ActorPermissions.taskIdProperty
                 + " = :"
                 + ActorPermissions.taskIdProperty
                 + " or ap."
                 + ActorPermissions.taskInstanceIdProperty
                 + " = :"
                 + ActorPermissions.taskInstanceIdProperty
                 + " and ap."
                 + ActorPermissions.variableIdentifierProperty
                 + " = :"
                 + ActorPermissions.variableIdentifierProperty)
})
public class ActorPermissions implements Serializable {
	
	private static final long serialVersionUID = 4768266953928292205L;
	
	public static final String getSetByTaskIdAndProcessRoleNames = "ActorPermissions.getSetByTaskIdAndProcessRoleNames";
	public static final String getSetByTaskIdOrTaskInstanceId = "ActorPermissions.getSetByTaskIdOrTaskInstanceId";
	public static final String getSetByProcessRoleNamesAndProcessInstanceIdPureRoles = "ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceId";
	public static final String getSetByProcessRoleNamesAndProcessInstanceIdForContacts = "ActorPermissions.getSetByProcessRoleNamesAndProcessInstanceIdForContacts";
	public static final String getSetByProcessInstanceIdAndContactPermissionsRolesNames = "ActorPermissions.getSetByProcessInstanceIdAndContactPermissionsRolesNames";
	public static final String getSetByProcessInstanceIdAndAccess = "ActorPermissions.getSetByProcessInstanceIdAndAccess";
	public static final String getSetByProcessInstanceIdAndCanSeeContacts = "ActorPermissions.getSetByProcessInstanceIdAndCanSeeContacts";
	public static final String getSetByProcessInstanceIdAndCanSeeComments = "ActorPermissions.getSetByProcessInstanceIdAndCanSeeComments";
	public static final String getSetByProcessInstanceIdAndCanWriteComments = "ActorPermissions.getSetByProcessInstanceIdAndCanWriteComments";
	public static final String getSetByProcessInstanceIdAndCanSeeAttachments = "ActorPermissions.getSetByProcessInstanceIdAndCanSeeAttachments";
	public static final String getSetByTaskIdOrTaskInstanceIdAndVariableIdentifier = "ActorPermissions.getSetByTaskIdOrTaskInstanceIdAndVariableIdentifier";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "actperm_id")
	private Long id;
	
	public static final String taskIdProperty = "taskId";
	@Column(name = "task_id")
	private Long taskId;
	
	public static final String taskInstanceIdProperty = "taskInstanceId";
	@Column(name = "task_instance_id")
	private Long taskInstanceId;
	
	public static final String variableIdentifierProperty = "variableIdentifier";
	@Column(name = "variable_identifier")
	private String variableIdentifier;
	
	public static final String roleNameProperty = "roleName";
	@Column(name = "process_role_name")
	private String roleName;
	
	public static final String readPermissionProperty = "readPermission";
	@Column(name = "has_read_permission")
	private Boolean readPermission;
	
	public static final String writePermissionProperty = "writePermission";
	@Column(name = "has_write_permission")
	private Boolean writePermission;
	
	public static final String modifyRightsPermissionProperty = "modifyRightsPermission";
	@Column(name = "can_modify_rights_permission")
	private Boolean modifyRightsPermission;
	
	public static final String caseHandlerPermissionProperty = "caseHandlerPermission";
	@Column(name = "case_handler_permission")
	private Boolean caseHandlerPermission;
	
	public static final String canSeeContactsOfRoleNameProperty = "canSeeContactsOfRoleName";
	@Column(name = "can_see_contacts_of_role_name")
	private String canSeeContactsOfRoleName;
	
	public static final String canSeeAttachmentsOfRoleNameProperty = "canSeeAttachmentsOfRoleName";
	@Column(name = "can_see_attachments_of_role")
	private String canSeeAttachmentsOfRoleName;
	
	public static final String canSeeContactsProperty = "canSeeContacts";
	@Column(name = "can_see_contacts")
	private Boolean canSeeContacts;
	
	public static final String canSeeCommentsProperty = "canSeeComments";
	@Column(name = "can_see_comments")
	private Boolean canSeeComments;
	
	public static final String canWriteCommentsProperty = "canWriteComments";
	@Column(name = "can_write_comments")
	private Boolean canWriteComments;
	
	public static final String canSeeAttachmentsProperty = "canSeeAttachments";
	@Column(name = "can_see_attachments")
	private Boolean canSeeAttachments;
	
	public static final String actorsProperty = "actors";
	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, mappedBy = Actor.actorPermissionsProperty, targetEntity = Actor.class)
	private List<Actor> actors;
	
	public List<Actor> getActors() {
		return actors;
	}
	
	public void setActors(List<Actor> actors) {
		this.actors = actors;
	}
	
	public void addActor(Actor actor) {
		List<Actor> actors = getActors();
		
		if (actors == null) {
			actors = new ArrayList<Actor>();
			setActors(actors);
		}
		
		actors.add(actor);
	}
	
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
	
	public String getCanSeeContactsOfRoleName() {
		return canSeeContactsOfRoleName;
	}
	
	public void setCanSeeContactsOfRoleName(String canSeeContactsOfRoleName) {
		this.canSeeContactsOfRoleName = canSeeContactsOfRoleName;
	}
	
	public Boolean getCaseHandlerPermission() {
		return caseHandlerPermission;
	}
	
	public void setCaseHandlerPermission(Boolean caseHandlerPermission) {
		this.caseHandlerPermission = caseHandlerPermission;
	}
	
	public Boolean getCanSeeContacts() {
		return canSeeContacts;
	}
	
	public void setCanSeeContacts(Boolean canSeeContacts) {
		this.canSeeContacts = canSeeContacts;
	}

	public Boolean getCanSeeComments() {
		return canSeeComments;
	}

	public void setCanSeeComments(Boolean canSeeComments) {
		this.canSeeComments = canSeeComments;
	}

	public Boolean getCanWriteComments() {
		return canWriteComments;
	}

	public void setCanWriteComments(Boolean canWriteComments) {
		this.canWriteComments = canWriteComments;
	}

	public Boolean getCanSeeAttachments() {
		return canSeeAttachments;
	}

	public void setCanSeeAttachments(Boolean canSeeAttachments) {
		this.canSeeAttachments = canSeeAttachments;
	}

	public String getCanSeeAttachmentsOfRoleName() {
		return canSeeAttachmentsOfRoleName;
	}

	public void setCanSeeAttachmentsOfRoleName(String canSeeAttachmentsOfRoleName) {
		this.canSeeAttachmentsOfRoleName = canSeeAttachmentsOfRoleName;
	}
	
}