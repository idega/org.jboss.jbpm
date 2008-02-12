package com.idega.jbpm.data;


import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/02/12 14:35:32 $ by $Author: civilis $
 */
@Entity
@Table(name="BPM_MANAGERS_PROCDEF")
@NamedQueries(
		{
			@NamedQuery(name=ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getByProcessDefinitionId, query="from ManagersTypeProcessDefinitionBind MTPDB where MTPDB.processDefinitionId = :"+ManagersTypeProcessDefinitionBind.processDefinitionIdParam),
			@NamedQuery(name=ManagersTypeProcessDefinitionBind.managersTypeProcessDefinitionBind_getAllProcDefs, query="select PD from org.jbpm.graph.def.ProcessDefinition PD, ManagersTypeProcessDefinitionBind MTPDB where PD.id = MTPDB.processDefinitionId")
		}
)
public class ManagersTypeProcessDefinitionBind implements Serializable {
	
	private static final long serialVersionUID = 9123064367761595198L;
	
	public static final String managersTypeProcessDefinitionBind_getByProcessDefinitionId = "managersTypeProcessDefinitionBind.getByProcessDefinitionId";
	public static final String managersTypeProcessDefinitionBind_getAllProcDefs = "managersTypeProcessDefinitionBind.getAllWProcDef";
	public static final String processDefinitionIdParam = "processDefinitionId";

//	TODO: process definition id should point to ProcessDefinition table. Managers type should point to BPM_MANAGERS_TYPES table (doesn't exist)
	@Id
	@Column(name="process_definition_id", nullable=false)
    private Long processDefinitionId;
	
	@Column(name="managers_type", nullable=false)
	private String managersType;

	public ManagersTypeProcessDefinitionBind() { }

	public Long getProcessDefinitionId() {
		return processDefinitionId;
	}

	public void setProcessDefinitionId(Long processDefinitionId) {
		
		if(this.processDefinitionId != null)
			throw new IllegalStateException("Process definition already set");
		
		this.processDefinitionId = processDefinitionId;
	}

	public String getManagersType() {
		return managersType;
	}

	public void setManagersType(String managersType) {
		this.managersType = managersType;
	}
}