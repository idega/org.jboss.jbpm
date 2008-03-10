package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.Table;

import com.idega.jbpm.identity.permission.Access;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/10 19:32:47 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_TASK_INSTANCE_ACCESSES")
@NamedQueries(
		{
		}
)
public class TaskInstanceAccess implements Serializable {

	private static final long serialVersionUID = -8875288664911164664L;
	
	@Id @GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name="ID")
    private Long id;
	
	public static final String taskInstanceIdProperty = "taskInstanceId";
    @Column(name="task_instance_id", nullable=false)
	private Long taskInstanceId;
	
	public static final String processRoleProperty = "processRole";
	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinColumn(name="process_role_fk", nullable=false)
	private ProcessRole processRole;
	
	@Column(name="has_read_access")
	private Boolean hasReadAccess;
	
	@Column(name="has_write_access")
	private Boolean hasWriteAccess;
	
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

	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public ProcessRole getProcessRole() {
		return processRole;
	}

	public void setProcessRole(ProcessRole processRole) {
		this.processRole = processRole;
	}

	public Long getTaskInstanceId() {
		return taskInstanceId;
	}

	public void setTaskInstanceId(Long taskInstanceId) {
		this.taskInstanceId = taskInstanceId;
	}
}