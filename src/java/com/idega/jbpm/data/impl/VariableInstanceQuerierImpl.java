package com.idega.jbpm.data.impl;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariableInstanceQuerierImpl extends DefaultSpringBean implements VariableInstanceQuerier {

	private static final String STANDARD_COLUMNS = "var.ID_ as varid, var.NAME_ as name, var.CLASS_ as type";
	private static final String FROM = " from JBPM_VARIABLEINSTANCE var";
	private static final String CLASS_CONDITION = " var.CLASS_ <> '" + VariableInstanceType.NULL.getTypeKeys().get(0) + "' ";
	private static final String CONDITION = " var.NAME_ is not null and" + CLASS_CONDITION;
	private static final String VAR_DEFAULT_CONDITION = " and" + CONDITION;
	private static final String PROCESS_INSTANCE_INNER_JOIN = " inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ ";
	private static final String PROCESS_INSTANCE_INNER_JOIN_EQUALS = PROCESS_INSTANCE_INNER_JOIN + "= ";
	private static final String PROCESS_DEFINITION_INNER_JOIN = " inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ ";
	
	private static final int COLUMNS = 3;
	private static final int FULL_COLUMNS = COLUMNS + 5;
	
	private String getSelectPart(String columns) {
		return getSelectPart(columns, Boolean.FALSE);
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
			query = getQuery(getSelectPart(selectColumns), FROM, PROCESS_INSTANCE_INNER_JOIN_EQUALS, "pi.ID_", PROCESS_DEFINITION_INNER_JOIN, "where", CONDITION,
					"and pd.NAME_ = '", processDefinitionName, "'");
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
			query = getQuery(getSelectPart(selectColumns), FROM, " where var.PROCESSINSTANCE_ = ", String.valueOf(processInstanceId), VAR_DEFAULT_CONDITION);
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
			query = getQuery(getSelectPart(getFullColumns(), false), FROM, ", jbpm_taskinstance task ", " where", procIdsIn, " and ", varNamesIn,
					" and var.TASKINSTANCE_ = task.ID_ and task.END_ is not null and ", CLASS_CONDITION, " order by var.TASKINSTANCE_");
			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables for process instance(s) : " + procIds +
					" and name(s): " + names, e);
		}
		
		return getConverted(data, FULL_COLUMNS);
	}

	public boolean isVariableStored(String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, false);
		if (ListUtil.isEmpty(variables)) {
			return false;
		}
		
		if (value instanceof String) {
			for (VariableInstanceInfo var: variables) {
				if (value.toString().equals(var.getValue())) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}
		
		return Boolean.TRUE;
	}
	
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, true);
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		Collection<Long> piIds = new ArrayList<Long>();
		for (VariableInstanceInfo variable: variables) {
			Long piId = null;
			if (value instanceof String) {
				if (isValueEquivalent(variable, value)) {
					piId = variable.getProcessInstanceId();
				}
			} else {
				piId = variable.getProcessInstanceId();
			}
			
			if (piId != null && !piIds.contains(piId)) {
				piIds.add(piId);
			}
		}
		
		return piIds;
	}
	
	public Collection<VariableInstanceInfo> getVariablesByNameAndValue(String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, false);
		if (ListUtil.isEmpty(variables) || !(value instanceof String)) {
			return variables;
		}
		
		Collection<VariableInstanceInfo> filtered = new ArrayList<VariableInstanceInfo>();
		for (VariableInstanceInfo variable: variables) {
			if (isValueEquivalent(variable, value)) {
				filtered.add(variable);
			}
		}
		
		return filtered;
	}
	
	private boolean isValueEquivalent(VariableInstanceInfo variable, Serializable value) {
		if (value instanceof String && variable instanceof VariableStringInstance && value.equals(((VariableStringInstance) variable).getValue())) {
			return true;
		}
		
		return false;
	}
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, Serializable value, boolean selectProcessInstanceId) {
		if (StringUtil.isEmpty(name) || value == null) {
			getLogger().warning("Variable name and/or value is not provided");
			return null;
		}

		String query = null;
		int columns = COLUMNS + (selectProcessInstanceId ? 2 : 1);
		List<Serializable[]> data = null;
		try {
			VariableInstanceType type = null;
			String columnName = "var.";
			if (value instanceof String) {
				value = null;
				columnName = columnName.concat("STRINGVALUE_");
				columnName = getSubstring(columnName);
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
			
			List<String> parts = new ArrayList<String>();
			parts.addAll(Arrays.asList(getSelectPart(STANDARD_COLUMNS, false), ", ", columnName, " as v ",
					selectProcessInstanceId ? ", var.PROCESSINSTANCE_ as piid" : CoreConstants.EMPTY,
					FROM, " where var.NAME_ = '", name, "' "));
			if (value != null) {
				parts.addAll(Arrays.asList(" and ", columnName, " = ", value.toString()));
			}
			parts.addAll(Arrays.asList(" and", getQueryParameters("var.CLASS_", type.getTypeKeys())));
			query = getQuery(ArrayUtil.convertListToArray(parts));
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
		
		Map<Number, VariableInstanceInfo> variables = new HashMap<Number, VariableInstanceInfo>();
		for (Serializable[] dataSet: data) {
			Number id = (Number) dataSet[0];
			String name = (String) dataSet[1];
			String type = (String) dataSet[2];
			
			if (id == null || name == null || type == null) {
				getLogger().warning("Unable to create variable from initial values: ID: " + id + ", name: " + name + ", class: " + type);
				continue;
			}
			
			Serializable value = null;
			Long piId = null;
			if (numberOfColumns > COLUMNS) {
				int index = COLUMNS;
				while (value == null && (index + 1) < numberOfColumns) {
					value = dataSet[index];
					index++;
				}
				
				int piIdIndex = -1;
				if (numberOfColumns == FULL_COLUMNS) {
					piIdIndex = numberOfColumns - 1;
				} else {
					piIdIndex = index;
				}
				if (piIdIndex >= index && piIdIndex < dataSet.length) {
					Serializable temp = dataSet[piIdIndex];
					if (temp instanceof Number) {
						piId = Long.valueOf(((Number) temp).longValue());
					}
				}
			}
			
			VariableInstanceInfo variable = variables.get(id);
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
					getLogger().warning("Unkown variable instance with id: " + id + ", name: '" + name + "', type: '" + type + "' and value: " + value);
				} else {
					variable.setProcessInstanceId(piId);
					variables.put(id, variable);
				}
			} else if (value != null) {
				variable.setValue(value);
			}
		}
		
		return variables.values();
	}
	
	private String getFullColumns() {
		return STANDARD_COLUMNS.concat(", ").concat(getSubstring("var.STRINGVALUE_")).concat(" as sv, var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov,")
			.concat(" var.DATEVALUE_ as dav, var.PROCESSINSTANCE_ as piid");
	}
	
	private String getSubstring(String column) {
		return "substr(".concat(column).concat(", 1, 255)");
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
			query = getQuery(getSelectPart(getFullColumns()), FROM, " where", getQueryParameters("var.NAME_", names), VAR_DEFAULT_CONDITION);
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