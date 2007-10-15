package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/10/15 16:16:24 $ by $Author: civilis $
 */
@Entity
@Table(name="CASES_JBPM_BINDINGS")
public class CasesJbpmBind implements Serializable {
	
	private static final long serialVersionUID = -3222584305636229751L;

	@Id
	@Column(name="process_definition_id")
    private Long procDefId;
	
	@Column(name="cases_category_id")
	private Long casesCategoryId;
	
	@Column(name="cases_type_id")
	private Long casesTypeId;
	
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
}
