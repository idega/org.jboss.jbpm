package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/04/17 23:58:33 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_AUTOLOADED_PROCDEFS")
@NamedQueries(
		{
		}
)
public class AutoloadedProcessDefinition implements Serializable {
	
	private static final long serialVersionUID = -5964009380377412821L;

	@Id
	@Column(name="process_definition_name")
    private String processDefinitionName;
	
	@Column(name="autoloaded_version", nullable=false)
	private Integer autoloadedVersion;
	
	@Column(name="is_autodeploy_permitted", nullable=false)
	private Boolean autodeployPermitted;

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public void setProcessDefinitionName(String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public Integer getAutoloadedVersion() {
		return autoloadedVersion;
	}

	public void setAutoloadedVersion(Integer autoloadedVersion) {
		this.autoloadedVersion = autoloadedVersion;
	}

	public Boolean getAutodeployPermitted() {
		return autodeployPermitted != null && autodeployPermitted;
	}
	
	public void setAutodeployPermitted(Boolean autodeployPermitted) {
		this.autodeployPermitted = autodeployPermitted;
	}
}