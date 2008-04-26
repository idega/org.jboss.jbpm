package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/26 02:48:33 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_ACTORS_PERMISSIONS")
@NamedQueries(
		{
			@NamedQuery(name=ActorPermissions.getSetByTaskIdAndProcessRole, query="from ActorPermissions ap where ap."+ActorPermissions.taskIdProperty+" = :"+ActorPermissions.taskIdProperty+" and "+ActorPermissions.processRoleProperty+" in (:"+ActorPermissions.processRoleProperty+")")
		}
)
public class ActorPermissions implements Serializable {

	private static final long serialVersionUID = 4768266953928292205L;
	
	public static final String getSetByTaskIdAndProcessRole = "ActorPermissions.getSetByTaskIdAndProcessRole";

	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="actperm_id")
	private Long id;
	
	public static final String taskIdProperty = "taskId";
	@Column(name="task_id", nullable=false)
	private Long taskId;
	
	public static final String taskInstanceIdProperty = "taskInstanceId";
	@Column(name="task_instance_id")
	private Long taskInstanceId;
	
	public static final String readPermissionProperty = "readPermission";
	@Column(name="has_read_permission", nullable=false)
	private Boolean readPermission;
	
	public static final String writePermissionProperty = "writePermission";
	@Column(name="has_write_permission", nullable=false)
	private Boolean writePermission;
	
	public static final String processRoleProperty = "processRole";
	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinColumn(name="process_role_actor_id", nullable=false)
	private ProcessRole processRole;
	
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
	public ProcessRole getProcessRole() {
		return processRole;
	}
	public void setProcessRole(ProcessRole processRole) {
		this.processRole = processRole;
	}
}