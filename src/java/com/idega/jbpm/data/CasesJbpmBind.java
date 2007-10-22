package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2007/10/22 15:40:01 $ by $Author: civilis $
 */
@Entity
@Table(name="CASES_JBPM_BINDINGS")
@NamedQuery(name="casesJbpmBind.simpleCasesProcessesDefinitionsQuery", query="select pd.id, pd.name from org.jbpm.graph.def.ProcessDefinition pd, CasesJbpmBind cb where pd.id = cb.procDefId")
public class CasesJbpmBind implements Serializable {
	
	private static final long serialVersionUID = -3222584305636229751L;

	@Id
	@Column(name="process_definition_id")
    private Long procDefId;
	
	@Column(name="cases_category_id")
	private Long casesCategoryId;
	
	@Column(name="cases_type_id")
	private Long casesTypeId;
	
	@Column(name="init_task_name")
	private String initTaskName;
	
	public Long getCasesCategoryId() {
		return casesCategoryId;
	}

	public void setCasesCategoryId(Long casesCategoryId) {
		this.casesCategoryId = casesCategoryId;
	}

	public Long getCasesTypeId() {
		return casesTypeId;
	}

	public void setCasesTypeId(Long casesTypeId) {
		this.casesTypeId = casesTypeId;
	}

	public CasesJbpmBind() { }

	public Long getProcDefId() {
		return procDefId;
	}

	public void setProcDefId(Long procDefId) {
		this.procDefId = procDefId;
	}

	public String getInitTaskName() {
		return initTaskName;
	}

	public void setInitTaskName(String initTaskName) {
		this.initTaskName = initTaskName;
	}
}