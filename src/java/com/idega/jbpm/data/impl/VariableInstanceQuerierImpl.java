package com.idega.jbpm.data.impl;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.data.SimpleQuerier;
import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.bean.VariableDateInstance;
import com.idega.jbpm.bean.VariableDefaultInstance;
import com.idega.jbpm.bean.VariableDoubleInstance;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.bean.VariableLongInstance;
import com.idega.jbpm.bean.VariableStringInstance;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariableInstanceQuerierImpl extends DefaultSpringBean implements VariableInstanceQuerier {

	private static final String STANDARD_COLUMNS = "var.NAME_ as name, var.CLASS_ as type";
	private static final String FROM = " from JBPM_VARIABLEINSTANCE var";
	private static final String CLASS_CONDITION = " var.CLASS_ <> '" + VariableInstanceType.NULL.getTypeKeys().get(0) + "' ";
	private static final String CONDITION = " var.NAME_ is not null and" + CLASS_CONDITION;
	private static final String VAR_DEFAULT_CONDITION = " and" + CONDITION;
	private static final String PROCESS_INSTANCE_INNER_JOIN = " inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ ";
	private static final String PROCESS_INSTANCE_INNER_JOIN_EQUALS = PROCESS_INSTANCE_INNER_JOIN + "= ";
	private static final String PROCESS_DEFINITION_INNER_JOIN = " inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ ";
	
	private static final int COLUMNS = 2;
	private static final int FULL_COLUMNS = COLUMNS + 5;
	
	private String getSelectPart(String columns) {
		return getSelectPart(columns, Boolean.TRUE);
	}
	
	private String getSelectPart(String columns, boolean distinct) {
		return "select ".concat(distinct ? "distinct " : CoreConstants.EMPTY).concat(columns);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessDefinition(String processDefinitionName, boolean full) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			getLogger().warning("Process definition name is not provided");
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns() : STANDARD_COLUMNS;
			int columns = full ? FULL_COLUMNS : COLUMNS;
			query = getQuery(getSelectPart(selectColumns), FROM, PROCESS_INSTANCE_INNER_JOIN_EQUALS, "pi.ID_", PROCESS_DEFINITION_INNER_JOIN,
					"where", CONDITION, "and pd.NAME_ = '", processDefinitionName, "'");
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process definition: " +
					processDefinitionName, e);
		}
		
		return getConverted(data, full ? FULL_COLUMNS : COLUMNS);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinition(String processDefinitionName) {
		return getVariablesByProcessDefinition(processDefinitionName, false);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName) {
		return getVariablesByProcessDefinition(processDefinitionName, true);
	}

	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId, boolean full) {
		if (processInstanceId == null) {
			getLogger().warning("Invalid ID of process instance");
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns() : STANDARD_COLUMNS;
			int columns = full ? FULL_COLUMNS : COLUMNS;
			query = getQuery(getSelectPart(selectColumns), FROM, " where var.PROCESSINSTANCE_ = ", String.valueOf(processInstanceId),
					VAR_DEFAULT_CONDITION);
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process instance: " +
					processInstanceId, e);
		}
		
		return getConverted(data, full ? FULL_COLUMNS : COLUMNS);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, false);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, true);
	}

	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names) {
		if (ListUtil.isEmpty(procIds)) {
			getLogger().warning("Process instance(s) unkown");
			return null;
		}
		if (ListUtil.isEmpty(names)) {
			getLogger().warning("Variable name(s) unknown");
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String procIdsIn = getQueryParameters("var.PROCESSINSTANCE_", procIds);
			String varNamesIn = getQueryParameters("var.NAME_", names);
			query = getQuery(getSelectPart(getFullColumns(), false), FROM, " where", procIdsIn, VAR_DEFAULT_CONDITION, " and", varNamesIn,
					" and var.TASKINSTANCE_ in (select t.ID_ from jbpm_taskinstance t where ", getQueryParameters("t.PROCINST_", procIds),
					" and t.END_ is not null) order by var.TASKINSTANCE_");
			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables for process instance(s) : " + procIds +
					" and name(s): " + names, e);
		}
		
		return getConverted(data, FULL_COLUMNS);
	}

	public Collection<VariableInstanceInfo> getVariablesByNameAndValue(String name, Serializable value) {
		if (StringUtil.isEmpty(name) || value == null) {
			getLogger().warning("Variable name and/or value is not provided");
			return null;
		}

		String query = null;
		int columns = COLUMNS + 1;
		List<Serializable[]> data = null;
		try {
			VariableInstanceType type = null;
			String columnName = "var.";
			if (value instanceof String) {
				columnName = columnName.concat("STRINGVALUE_");
				value = CoreConstants.QOUTE_SINGLE_MARK.concat(value.toString()).concat(CoreConstants.QOUTE_SINGLE_MARK);
				type = VariableInstanceType.STRING;
			} else if (value instanceof Long) {
				columnName = columnName.concat("LONGVALUE_");
				type = VariableInstanceType.LONG;
			} else if (value instanceof Double) {
				columnName = columnName.concat("DOUBLEVALUE_");
				type = VariableInstanceType.DOUBLE;
			} else if (value instanceof Timestamp) {
				columnName = columnName.concat("DATEVALUE_");
				type = VariableInstanceType.DATE;
			} else {
				getLogger().warning("Unsupported type of value: " + value + ", " + value.getClass());
				return null;
			}
			
			query = getQuery(getSelectPart(STANDARD_COLUMNS, false), ", ", columnName, " as v ", FROM, " where var.NAME_ = '", name, "' and", columnName,
					" like ", value.toString(), " and", getQueryParameters("var.CLASS_", type.getTypeKeys()));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables by name: " + name + " and value: " + value, e);
		}
		
		return getConverted(data, columns);
	}

	private Collection<VariableInstanceInfo> getConverted(List<Serializable[]> data, int numberOfColumns) {
		if (ListUtil.isEmpty(data)) {
			return null;
		}
		
		List<String> addedVariables = new ArrayList<String>();
		List<VariableInstanceInfo> variables = new ArrayList<VariableInstanceInfo>(data.size());
		for (Serializable[] dataSet: data) {
			String name = (String) dataSet[0];
			String type = (String) dataSet[1];
			
			Serializable value = null;
			Long piId = null;
			if (numberOfColumns > COLUMNS) {
				int index = COLUMNS;
				while (value == null && (index + 1) < numberOfColumns) {
					value = dataSet[index];
					index++;
				}
				
				if (numberOfColumns == FULL_COLUMNS) {
					Serializable id = dataSet[numberOfColumns - 1];
					if (id instanceof Number) {
						piId = Long.valueOf(((Number) id).longValue());
					} else if (id != null) {
						getLogger().warning("Unable to convert " + id + " ("+id.getClass()+") to Long!");
					}
				}
			}
			
			VariableInstanceInfo variable = null;
			
			if (piId != null && addedVariables.contains(name)) {
				variable = findVariable(variables, piId, name);
			}
			
			if (variable == null) {
				if (value == null) {
					variable = new VariableDefaultInstance(name, type);
				} else if (value instanceof String) {
					variable = new VariableStringInstance(name, (String) value);
				} else if (value instanceof Long) {
					variable = new VariableLongInstance(name, (Long) value);
				} else if (value instanceof Double) {
					variable = new VariableDoubleInstance(name, (Double) value);
				} else if (value instanceof Timestamp) {
					variable = new VariableDateInstance(name, (Timestamp) value);
				} else if (value instanceof Byte[]) {
					variable = new VariableByteArrayInstance(name, (Byte[]) value);
				}
				
				if (variable == null) {
					getLogger().warning("Unkown variable instance with name: '" + name + "', type: '" + type + "' and value: " + value);
				} else {
					variable.setProcessInstanceId(piId);
					variables.add(variable);
					addedVariables.add(name);
				}
			} else {
				variable.setValue(value);
			}
		}
		
		return variables;
	}
	
	private VariableInstanceInfo findVariable(List<VariableInstanceInfo> variables, Long piId, String name) {
		if (ListUtil.isEmpty(variables) || piId == null || name == null) {
			return null;
		}
		
		VariableInstanceInfo variable = null;
		for (Iterator<VariableInstanceInfo> varsIter = variables.iterator(); (varsIter.hasNext() && variable == null);) {
			variable = varsIter.next();
			
			if (variable.getProcessInstanceId() != null && variable.getProcessInstanceId().longValue() == piId.longValue() && variable.getName() != null &&
					variable.getName().equals(name)) {
			} else {
				variable = null;
			}
		}
		return variable;
	}
	
	private String getFullColumns() {
		return STANDARD_COLUMNS.concat(", var.STRINGVALUE_ as sv, var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.PROCESSINSTANCE_" +
				" as piid");
	}
	
	private String getQueryParameters(String columnName, Collection<? extends Serializable> values) {
		String params = StringUtil.isEmpty(columnName) ? " (" : " ".concat(columnName).concat(" in (");
		for (Iterator<? extends Serializable> iter = values.iterator(); iter.hasNext();) {
			Serializable value = iter.next();
			boolean isString = value instanceof String;
			if (isString) {
				params = params.concat(CoreConstants.QOUTE_SINGLE_MARK);
			}
			params = params.concat(value.toString());
			if (isString) {
				params = params.concat(CoreConstants.QOUTE_SINGLE_MARK);
			}
			
			if (iter.hasNext()) {
				params = params.concat(CoreConstants.COMMA).concat(CoreConstants.SPACE);
			}
		}
		params = params.concat(CoreConstants.BRACKET_RIGHT).concat(CoreConstants.SPACE);
		return params;
	}

	public Collection<VariableInstanceInfo> getVariablesByNames(List<String> names) {
		if (ListUtil.isEmpty(names)) {
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			query = getQuery(getSelectPart(getFullColumns()), FROM, VAR_DEFAULT_CONDITION, " and", getQueryParameters("var.NAME_", names));
			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables by names: " + names, e);
		}
		
		return getConverted(data, FULL_COLUMNS);
	}
	
	private String getQuery(String... parts) {
		StringBuffer query = new StringBuffer();
		for (String part: parts) {
			query.append(part);
		}
		return query.toString();
	}
}