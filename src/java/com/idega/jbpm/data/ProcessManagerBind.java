package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/11/19 21:28:34 $ by $Author: civilis $
 */
@Entity
@Table(name = "BPM_PROCESS_MANAGERS")
@NamedQueries( { @NamedQuery(name = ProcessManagerBind.getByProcessName, query = "from ProcessManagerBind pm where pm."
		+ ProcessManagerBind.processNameProp
		+ " = :"
		+ ProcessManagerBind.processNameProp) })
public class ProcessManagerBind implements Serializable {

	private static final long serialVersionUID = 1748907927777733985L;

	public static final String getByProcessName = "ProcessManagerBind.getByProcessName";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id_")
	private Long id;

	@Column(name = "managers_type", nullable = false)
	private String managersType;

	// TODO: process definition id should point to ProcessDefinition table.
	public static final String processNameProp = "processName";
	@Column(name = "process_definition_name", nullable = false, unique = true)
	private String processName;

	public ProcessManagerBind() {
	}

	public String getManagersType() {
		return managersType;
	}

	public void setManagersType(String managersType) {
		this.managersType = managersType;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {

		if (this.processName != null) {

			Logger.getLogger(getClass().getName()).log(
					Level.WARNING,
					"Tried to set process name, but process name already set. Process name="
							+ this.processName);
		} else
			this.processName = processName;
	}

	public Long getId() {
		return id;
	}
}