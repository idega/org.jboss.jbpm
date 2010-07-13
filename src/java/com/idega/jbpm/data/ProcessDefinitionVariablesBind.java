package com.idega.jbpm.data;

import java.io.Serializable;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

@Entity
@Table(name="BPM_PROC_DEF_VARS")
@NamedQueries({
	@NamedQuery(name=ProcessDefinitionVariablesBind.QUERY_SELECT_ALL, query="from ProcessDefinitionVariablesBind"),
	@NamedQuery(name=ProcessDefinitionVariablesBind.QUERY_SELECT_BY_PROCESS_DEFINITION_NAMES, query="select pdvb from ProcessDefinitionVariablesBind pdvb " +
			"where pdvb.processDefinition in (:" + ProcessDefinitionVariablesBind.PARAM_PROC_DEF_NAMES + ")")
})
public class ProcessDefinitionVariablesBind implements Serializable {

	private static final long serialVersionUID = -917116235662898555L;

	public static final String QUERY_SELECT_ALL = "ProcessDefinitionVariablesBind.getAll";
	public static final String QUERY_SELECT_BY_PROCESS_DEFINITION_NAMES = "ProcessDefinitionVariablesBind.getByProcessDefinitionNames";
	
	public static final String PARAM_PROC_DEF_NAMES = "processDefinition_parameter";
	
	private int hashCode;
	
	public ProcessDefinitionVariablesBind() {
		hashCode = new Random().nextInt();
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id")
	private Long id;
	
	@Index(columnNames={"process_definition_name"}, name="bpm_proc_def_vars_index1")
	@Column(name="process_definition_name")
	private String processDefinition;
	
	@Column(name="variable_name")
	private String variableName;
	
	@Column(name="variable_type")
	private String variableType;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProcessDefinition() {
		return processDefinition;
	}

	public void setProcessDefinition(String processDefinition) {
		this.processDefinition = processDefinition;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public String getVariableType() {
		return variableType;
	}

	public void setVariableType(String variableType) {
		this.variableType = variableType;
	}
	
	public int getHashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return getVariableName() + "@" + getProcessDefinition();
	}
}