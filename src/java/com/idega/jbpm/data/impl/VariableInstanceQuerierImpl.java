package com.idega.jbpm.data.impl;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.cache.IWCacheManager2;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.bean.VariableDateInstance;
import com.idega.jbpm.bean.VariableDefaultInstance;
import com.idega.jbpm.bean.VariableDoubleInstance;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceInfoComparator;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.bean.VariableLongInstance;
import com.idega.jbpm.bean.VariableStringInstance;
import com.idega.jbpm.data.BPMVariableData;
import com.idega.jbpm.data.ProcessDefinitionVariablesBind;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.events.VariableCreatedEvent;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.reflect.MethodInvoker;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariableInstanceQuerierImpl extends GenericDaoImpl implements
        VariableInstanceQuerier, ApplicationListener {
	
	private static final Logger LOGGER = Logger
	        .getLogger(VariableInstanceQuerierImpl.class.getName());
	
	private static final String VARIABLES_TABLE = "JBPM_VARIABLEINSTANCE";
	private static final String STANDARD_COLUMNS = "var.ID_ as varid, var.NAME_ as name, var.CLASS_ as type";
	private static final String FROM = " from " + VARIABLES_TABLE + " var";
	private static final String CLASS_CONDITION = " var.CLASS_ <> '"
	        + VariableInstanceType.NULL.getTypeKeys().get(0) + "' ";
	private static final String NAME_CONDITION = " var.NAME_ is not null ";
	private static final String CONDITION = NAME_CONDITION + " and"
	        + CLASS_CONDITION;
	private static final String VAR_DEFAULT_CONDITION = " and" + CONDITION;
	private static final String NOT_STRING_VALUES = " var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.PROCESSINSTANCE_ as piid";
	
	private static final String MIRRROW_TABLE_ALIAS = "mirrow";
	
	private static final int COLUMNS = 3;
	private static final int FULL_COLUMNS = COLUMNS + 5;
	
	private static final Random ID_GENERATOR = new Random();
	
	private String getSelectPart(String columns) {
		return getSelectPart(columns, Boolean.FALSE);
	}
	
	private String getSelectPart(String columns, boolean distinct) {
		return "select ".concat(distinct ? "distinct " : CoreConstants.EMPTY).concat(columns);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinitionNaiveWay(
	        String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName,
		    false);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessDefinitionNaiveWay(
	        String processDefinitionName, boolean full) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			LOGGER.warning("Process definition name is not provided");
			return null;
		}
		
		int columns = full ? FULL_COLUMNS : 2;
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns()
			        : "var.NAME_ as name, var.CLASS_ as type";
			query = getQuery(
			    getSelectPart(selectColumns, !full),
			    FROM,
			    " inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ = pi.ID_ ",
			    "inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ where ",
			    CONDITION, " and pd.NAME_ = '", processDefinitionName, "'");
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(
			    Level.WARNING,
			    "Error executing query: '"
			            + query
			            + "'. Error getting variable instances by process definition: "
			            + processDefinitionName, e);
		}
		
		return getConverted(data, columns);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinition(
	        String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			LOGGER.warning("Process definition name is not provided");
			return null;
		}
		
		List<ProcessDefinitionVariablesBind> binds = getResultList(
		    ProcessDefinitionVariablesBind.QUERY_SELECT_BY_PROCESS_DEFINITION_NAMES,
		    ProcessDefinitionVariablesBind.class,
		    new Param(ProcessDefinitionVariablesBind.PARAM_PROC_DEF_NAMES,
		            Arrays.asList(processDefinitionName)));
		
		return getConverted(binds);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(
	        String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName,
		    true);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(
	        Long processInstanceId, boolean full) {
		if (processInstanceId == null) {
			LOGGER.warning("Invalid ID of process instance");
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns() : STANDARD_COLUMNS;
			int columns = full ? FULL_COLUMNS : COLUMNS;
			query = getQuery(getSelectPart(selectColumns), getFromClause(full),
			    " where var.PROCESSINSTANCE_ = ",
			    String.valueOf(processInstanceId), VAR_DEFAULT_CONDITION,
			    getMirrowTableCondition(full));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(
			    Level.WARNING,
			    "Error executing query: '"
			            + query
			            + "'. Error getting variable instances by process instance: "
			            + processInstanceId, e);
		}
		
		return getConverted(data, full ? FULL_COLUMNS : COLUMNS);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(
	        Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, false);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, true);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names) {
		return getVariablesByProcessInstanceIdAndVariablesNames(procIds, names, true);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names, boolean checkTaskInstance) {
		return getVariablesByProcessInstanceIdAndVariablesNames(procIds, names, checkTaskInstance, true);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds, boolean checkTaskInstance,
			boolean addEmptyVars) {
		return getVariablesByProcessInstanceIdAndVariablesNames(names, procIds, checkTaskInstance, addEmptyVars, true);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds, boolean checkTaskInstance,
			boolean addEmptyVars, boolean mirrowData) {
		
		if (ListUtil.isEmpty(procIds)) {
			LOGGER.warning("Process instance(s) unkown");
			return null;
		}
		if (ListUtil.isEmpty(names)) {
			LOGGER.warning("Variable name(s) unknown");
		}
		
		List<String> notStringVariables = new ArrayList<String>();
		for (String name : names) {
			if (!name.startsWith("string_")) {
				notStringVariables.add(name);
			}
		}
		List<String> stringVariables = new ArrayList<String>(names);
		if (!ListUtil.isEmpty(notStringVariables)) {
			stringVariables.removeAll(notStringVariables);
		}
		
		Collection<VariableInstanceInfo> stringVars = getVariablesByProcessInstanceIdAndVariablesNames(procIds, stringVariables, mirrowData, checkTaskInstance);
		Collection<VariableInstanceInfo> otherVars = getVariablesByProcessInstanceIdAndVariablesNames(procIds, notStringVariables, false, checkTaskInstance);
		
		Map<Integer, VariableInstanceInfo> vars = new HashMap<Integer, VariableInstanceInfo>();
		fillMapWithData(vars, names, stringVars);
		fillMapWithData(vars, names, otherVars);
		
		List<VariableInstanceInfo> variables = null;
		if (vars.size() > 0) {
			variables = new ArrayList<VariableInstanceInfo>();
			if (addEmptyVars) {
				for (int i = 0; i < names.size(); i++) {
					VariableInstanceInfo info = vars.get(i);
					if (info == null) {
						info = getEmptyVariable(names.get(i));
						if (info == null) {
							throw new RuntimeException();
						} else {
							vars.put(getKey(i, info.getProcessInstanceId()), info);
						}
					}
				}
			}	
			
			List<Integer> keys = new ArrayList<Integer>(vars.keySet());
			Collections.sort(keys);
			for (Integer key : keys) {
				variables.add(vars.get(key));
			}
		}
		
		return variables;
	}
	
	private VariableInstanceInfo getEmptyVariable(String name) {
		if (name.startsWith(VariableInstanceType.STRING.getPrefix())) {
			return new VariableStringInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix())) {
			return new VariableByteArrayInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.DATE.getPrefix())) {
			return new VariableDateInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.DOUBLE.getPrefix())) {
			return new VariableDoubleInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.LONG.getPrefix())) {
			return new VariableLongInstance(name, null);
		} else {
			LOGGER.warning("Can not resolve variable type from name: " + name + ". Will use default one");
			return VariableInstanceInfo.getDefaultVariable(name);
		}
	}
	
	private void fillMapWithData(Map<Integer, VariableInstanceInfo> vars, List<String> names, Collection<VariableInstanceInfo> variables) {
		if (ListUtil.isEmpty(variables)) {
			return;
		}
		
		List<String> addedVars = new ArrayList<String>();
		for (VariableInstanceInfo var : variables) {
			String name = var.getName();
			Long processInstance = var.getProcessInstanceId();
			int key = getKey(names.indexOf(name), processInstance);
			if (processInstance != null && addedVars.contains(name + processInstance)) {
				VariableInstanceInfo addedVar = vars.get(key);
				if (addedVar.getValue() != null)
					continue;	//	Added variable has value, it's OK
			} else {
				addedVars.add(name + processInstance);
			}
			
			vars.put(key, var);
		}
	}
	
	private int getKey(int index, Long piId) {
		return piId == null ? index : index * 1000000000 + piId.intValue();
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names, boolean mirrow,
			boolean checkTaskInstance) {
		
		if (ListUtil.isEmpty(names)) {
			return null;
		}
		
		boolean anyStringColumn = false;
		for (Iterator<String> namesIter = names.iterator(); (!anyStringColumn && namesIter.hasNext());) {
			anyStringColumn = namesIter.next().contains(VariableInstanceType.STRING.getPrefix());
		}
		
		String query = null;
		List<Serializable[]> data = null;
		int columns = anyStringColumn ? FULL_COLUMNS : FULL_COLUMNS - 1;
		try {
			String procIdsIn = getQueryParameters("var.PROCESSINSTANCE_", procIds);
			String varNamesIn = getQueryParameters("var.NAME_", names);
			query = getQuery(getSelectPart(anyStringColumn ? getFullColumns(mirrow, false) : getAllButStringColumn(), false), getFromClause(true, mirrow),
					checkTaskInstance ? ", jbpm_taskinstance task " : CoreConstants.EMPTY, " where ", procIdsIn, " and ", varNamesIn,
					checkTaskInstance ? " and var.TASKINSTANCE_ = task.ID_ and task.END_ is not null " : CoreConstants.EMPTY, " and ", CLASS_CONDITION,
					mirrow ? getMirrowTableCondition(true) : CoreConstants.EMPTY, " order by var.TASKINSTANCE_");
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query +
					"'. Error getting variables for process instance(s) : " + procIds + " and name(s): " + names, e);
		}
		
		return getConverted(data, columns);
	}
	
	public boolean isVariableStored(String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(
		    name, value, false);
		if (ListUtil.isEmpty(variables)) {
			return false;
		}
		
		if (value instanceof String) {
			for (VariableInstanceInfo var : variables) {
				if (value.toString().equals(var.getValue())) {
					return Boolean.TRUE;
				}
			}
			return Boolean.FALSE;
		}
		
		return Boolean.TRUE;
	}
	
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(
	        String name, Serializable value) {
		VariableInstanceInfo cachedVariable = getCachedVariable(name, value);
		if (cachedVariable != null
		        && cachedVariable.getProcessInstanceId() != null) {
			return Arrays.asList(cachedVariable.getProcessInstanceId());
		}
		
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(
		    name, value, true);
		if (ListUtil.isEmpty(variables)) {
			return null;
		}
		
		Collection<Long> piIds = new ArrayList<Long>();
		for (VariableInstanceInfo variable : variables) {
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
	
	public Collection<VariableInstanceInfo> getVariablesByNameAndValue(
	        String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(
		    name, value, false);
		if (ListUtil.isEmpty(variables) || !(value instanceof String)) {
			return variables;
		}
		
		Collection<VariableInstanceInfo> filtered = new ArrayList<VariableInstanceInfo>();
		for (VariableInstanceInfo variable : variables) {
			if (isValueEquivalent(variable, value)) {
				filtered.add(variable);
			}
		}
		
		return filtered;
	}
	
	private boolean isValueEquivalent(VariableInstanceInfo variable,
	        Serializable value) {
		if (value instanceof String
		        && variable instanceof VariableStringInstance) {
			String searchValue = (String) value;
			String variableValue = ((VariableStringInstance) variable)
			        .getValue();
			if (variableValue == null) {
				return false;
			}
			
			if (searchValue.equals(variableValue)) {
				return true;
			}
			
			Locale locale = CoreUtil.getCurrentLocale();
			searchValue = searchValue.toLowerCase(locale);
			variableValue = variableValue.toLowerCase(locale);
			return searchValue.equals(variableValue);
		}
		
		return false;
	}
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(
	        String name, Serializable value, boolean selectProcessInstanceId) {
		if (StringUtil.isEmpty(name) || value == null) {
			LOGGER.warning("Variable name and/or value is not provided");
			return null;
		}
		
		String query = null;
		int columns = COLUMNS + (selectProcessInstanceId ? 2 : 1);
		List<Serializable[]> data = null;
		try {
			boolean stringColumn = false;
			VariableInstanceType type = null;
			String columnName = "var.";
			if (value instanceof String) {
				stringColumn = true;
				columnName = columnName.concat(getStringValueColumn());
				columnName = getStringValueColumn();
				columnName = isDataMirrowed() ? columnName
				        : getSubstring(columnName);
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
				LOGGER.warning("Unsupported type of value: " + value + ", "
				        + value.getClass());
				return null;
			}
			
			List<String> parts = new ArrayList<String>();
			parts.addAll(Arrays.asList(getSelectPart(STANDARD_COLUMNS, false),
			    ", ", columnName, " as v ",
			    selectProcessInstanceId ? ", var.PROCESSINSTANCE_ as piid"
			            : CoreConstants.EMPTY, getFromClause(stringColumn),
			    " where var.NAME_ = '", name, "' and ", columnName, " "));
			if (stringColumn) {
				parts.addAll(Arrays.asList(isDataMirrowed() ? "=" : "like",
				    " '", value.toString(), "'"));
			} else {
				parts.addAll(Arrays.asList("= ", value.toString()));
			}
			parts.addAll(Arrays.asList(" and",
			    getQueryParameters("var.CLASS_", type.getTypeKeys()),
			    stringColumn ? getMirrowTableCondition() : CoreConstants.EMPTY));
			query = getQuery(ArrayUtil.convertListToArray(parts));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query
			        + "'. Error getting variables by name: " + name
			        + " and value: " + value, e);
		}
		
		return getConverted(data, columns);
	}
	
	private Number getRandomId(Set<Number> existingIds) {
		long id = ID_GENERATOR.nextLong();
		if (ListUtil.isEmpty(existingIds)) {
			return id;
		}
		
		if (existingIds.contains(id)) {
			return getRandomId(existingIds);
		}
		
		return id;
	}
	
	private Collection<VariableInstanceInfo> getConverted(List<ProcessDefinitionVariablesBind> binds) {
		if (ListUtil.isEmpty(binds)) {
			return null;
		}
		
		List<String> variables = new ArrayList<String>();
		List<Serializable[]> data = new ArrayList<Serializable[]>();
		for (ProcessDefinitionVariablesBind bind : binds) {
			String variableName = bind.getVariableName();
			if (StringUtil.isEmpty(variableName) || variables.contains(variableName)) {
				continue;
			}
			
			Serializable[] dataSet = new Serializable[2];
			dataSet[0] = variableName;
			dataSet[1] = bind.getVariableType();
			data.add(dataSet);
			variables.add(variableName);
		}
		
		return getConverted(data, 2);
	}
	
	private Collection<VariableInstanceInfo> getConverted(List<Serializable[]> data, int numberOfColumns) {
		if (ListUtil.isEmpty(data)) {
			return null;
		}
		
		boolean sort = true;
		Map<Number, VariableInstanceInfo> variables = new HashMap<Number, VariableInstanceInfo>();
		for (Serializable[] dataSet : data) {
			int startValues = 0;
			Number id = null;
			String name = null;
			
			Serializable idOrName = dataSet[startValues];
			if (idOrName instanceof Number) {
				id = (Number) idOrName;
			} else if (idOrName instanceof String) {
				name = (String) idOrName;
			}
			if (id != null || name != null) {
				startValues++;
			}
			if (id == null) {
				sort = false;
				id = getRandomId(variables.keySet()); // Generate ID
			}
			if (name == null) {
				name = (String) dataSet[startValues];
				startValues++;
			}
			String type = (String) dataSet[startValues];
			
			if (id == null || name == null || type == null) {
				LOGGER.warning("Unable to create variable from initial values (" + dataSet + "): ID: " + id + ", name: " + name + ", class: " + type);
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
				} else if (value instanceof String || VariableInstanceType.STRING.getTypeKeys().contains(type)) {
					variable = new VariableStringInstance(name, value);
				} else if (value instanceof Long) {
					variable = new VariableLongInstance(name, (Long) value);
				} else if (value instanceof Double) {
					variable = new VariableDoubleInstance(name, (Double) value);
				} else if (value instanceof Timestamp) {
					variable = new VariableDateInstance(name, (Timestamp) value);
				} else if (value instanceof Date) {
					variable = new VariableDateInstance(name, new Timestamp(((Date) value).getTime()));
				} else if (value instanceof Byte[] || VariableInstanceType.BYTE_ARRAY.getTypeKeys().contains(type)) {
					variable = new VariableByteArrayInstance(name, value);
				} else {
					// Try to execute custom methods
					java.sql.Date date = getValueFromCustomMethod(value, "dateValue");
					if (date != null) {
						variable = new VariableDateInstance(name, new Timestamp(date.getTime()));
					}
				}
				
				if (variable == null) {
					LOGGER.warning("Unkown variable instance with id: " + id + ", name: '" + name + "', type: '" + type + "' and value: " + value);
				} else {
					variable.setId(id.longValue());
					variable.setProcessInstanceId(piId);
					variables.put(id, variable);
				}
			} else if (value != null) {
				variable.setValue(value);
			}
		}
		
		if (sort) {
			List<VariableInstanceInfo> vars = new ArrayList<VariableInstanceInfo>(variables.values());
			Collections.sort(vars, new VariableInstanceInfoComparator());
			return vars;
		}
		return variables.values();
	}
	
	@SuppressWarnings("unchecked")
	private <T> T getValueFromCustomMethod(Object target, String method) {
		try {
			return (T) MethodInvoker.getInstance()
			        .invokeMethodWithNoParameters(target, method);
		} catch (Exception e) {
			String message = "Error invoking method " + method + " on object: "
			        + target;
			LOGGER.log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}
		return null;
	}
	
	private String getFullColumns() {
		return getFullColumns(isDataMirrowed(), false);	//	TODO: is this right?
	}

	private String getFullColumns(boolean mirrow, boolean substring) {
		String columns = STANDARD_COLUMNS.concat(", ");
		String valueColumn = getStringValueColumn(mirrow);
		valueColumn = mirrow ? valueColumn : substring ? getSubstring(valueColumn) : valueColumn;
		return columns.concat(valueColumn).concat(" as sv, ").concat(NOT_STRING_VALUES);
	}
	
	private String getAllButStringColumn() {
		return STANDARD_COLUMNS.concat(", ").concat(NOT_STRING_VALUES);
	}
	
	private String getSubstring(String column) {
		return "substr(".concat(column).concat(", 1, 255)");
	}
	
	private String getQueryParameters(String columnName,
	        Collection<? extends Serializable> values) {
		return getQueryParameters(columnName, values, Boolean.FALSE);
	}
	
	private String getQueryParameters(String columnName,
	        Collection<? extends Serializable> values, boolean notEquals) {
		String params = CoreConstants.EMPTY;
		if (values.size() == 1) {
			params = StringUtil.isEmpty(columnName) ? CoreConstants.SPACE
			        : CoreConstants.SPACE.concat(columnName);
			params = params.concat(notEquals ? " <> " : " = ");
			Serializable value = values.iterator().next();
			boolean isString = value instanceof String;
			if (isString) {
				params = params.concat(CoreConstants.QOUTE_SINGLE_MARK);
			}
			params = params.concat(String.valueOf(value));
			if (isString) {
				params = params.concat(CoreConstants.QOUTE_SINGLE_MARK);
			}
		} else {
			params = StringUtil.isEmpty(columnName) ? " ("
			        : CoreConstants.SPACE.concat(columnName)
			                .concat(notEquals ? " not " : CoreConstants.EMPTY)
			                .concat(" in (");
			for (Iterator<? extends Serializable> iter = values.iterator(); iter
			        .hasNext();) {
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
					params = params.concat(CoreConstants.COMMA).concat(
					    CoreConstants.SPACE);
				}
			}
			params = params.concat(CoreConstants.BRACKET_RIGHT);
		}
		
		params.concat(CoreConstants.SPACE);
		return params;
	}
	
	public Collection<VariableInstanceInfo> getVariablesByNames(
	        List<String> names) {
		if (ListUtil.isEmpty(names)) {
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			query = getQuery(getSelectPart(getFullColumns()), getFromClause(),
			    " where", getQueryParameters("var.NAME_", names),
			    VAR_DEFAULT_CONDITION, getMirrowTableCondition());
			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query
			        + "'. Error getting variables by names: " + names, e);
		}
		
		return getConverted(data, FULL_COLUMNS);
	}
	
	private String getQuery(String... parts) {
		StringBuffer query = new StringBuffer();
		for (String part : parts) {
			query.append(part);
		}
		return query.toString();
	}
	
	public static final boolean isDataMirrowed() {
		return IWMainApplication.getDefaultIWMainApplication().getSettings()
		        .getBoolean("jbpm_variables_mirrowed", Boolean.TRUE);
	}
	
	private String getStringValueColumn() {
		return getStringValueColumn(isDataMirrowed());
	}
	private String getStringValueColumn(boolean mirrow) {
		return mirrow ? MIRRROW_TABLE_ALIAS.concat(".stringvalue") : "var.stringvalue_";
	}
	
	private String getMirrowTableCondition() {
		return getMirrowTableCondition(true);
	}
	
	private String getMirrowTableCondition(boolean full) {
		return (full && isDataMirrowed()) ? " and var.ID_ = "
		        + MIRRROW_TABLE_ALIAS + ".variable_id " : CoreConstants.EMPTY;
	}
	
	private String getFromClause() {
		return getFromClause(true);
	}
	
	private String getFromClause(boolean full) {
		return getFromClause(full, isDataMirrowed());
	}
	
	private String getFromClause(boolean full, boolean mirrow) {
		String from = FROM;
		if (full && mirrow) {
			from = from.concat(", ").concat(BPMVariableData.TABLE_NAME)
			        .concat(" ").concat(MIRRROW_TABLE_ALIAS);
		}
		return from;
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(
	        List<Long> processInstanceIds) {
		return getFullVariablesByProcessInstanceIdsNaiveWay(processInstanceIds,
		    null);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(
	        List<Long> processInstanceIds, List<Long> existingVars) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}
		
		int columns = COLUMNS + 2;
		List<Serializable[]> data = null;
		String query = getQuery(
		    "select ",
		    STANDARD_COLUMNS,
		    ", ",
		    getSubstring("var.STRINGVALUE_"),
		    " as sv, var.PROCESSINSTANCE_ ",
		    FROM,
		    " where ",
		    getQueryParameters("var.PROCESSINSTANCE_", processInstanceIds),
		    " and ",
		    NAME_CONDITION + " and ",
		    getQueryParameters("var.CLASS_",
		        VariableInstanceType.STRING.getTypeKeys()));
		if (!ListUtil.isEmpty(existingVars)) {
			query = query.concat(" and ").concat(
			    getQueryParameters("var.ID_", existingVars, true));
		}
		try {
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
			    "Error getting variables by process instance IDs. Query: "
			            + query, e);
		}
		
		return getConverted(data, columns);
	}
	
	public void loadVariable(String variableName) {
		if (StringUtil.isEmpty(variableName)) {
			LOGGER.warning("Variable name is not provided!");
			return;
		}
		
		Collection<VariableInstanceInfo> vars = getVariablesByNames(Arrays
		        .asList(variableName));
		if (vars == null) {
			return;
		}
		
		Map<String, List<VariableInstanceInfo>> cache = getVariablesCache();
		List<VariableInstanceInfo> variables = cache.get(variableName);
		if (variables == null) {
			variables = new ArrayList<VariableInstanceInfo>();
			cache.put(variableName, variables);
		}
		for (VariableInstanceInfo info : vars) {
			Serializable value = info.getValue();
			if (value == null) {
				continue;
			}
			
			variables.add(info);
		}
	}
	
	private VariableInstanceInfo getCachedVariable(String name,
	        Serializable value) {
		if (StringUtil.isEmpty(name) || value == null) {
			return null;
		}
		
		Map<String, List<VariableInstanceInfo>> cache = getVariablesCache();
		List<VariableInstanceInfo> vars = cache.get(name);
		if (ListUtil.isEmpty(vars)) {
			return null;
		}
		
		VariableInstanceInfo variable = null;
		for (Iterator<VariableInstanceInfo> varsIterator = vars.iterator(); (variable == null && varsIterator
		        .hasNext());) {
			VariableInstanceInfo var = varsIterator.next();
			
			if (var.getValue() != null && var instanceof VariableStringInstance
			        && value.toString().equals(var.getValue())) {
				variable = var;
			}
		}
		
		return variable;
	}
	
	private Map<String, List<VariableInstanceInfo>> getVariablesCache() {
		IWMainApplication iwma = IWMainApplication
		        .getDefaultIWMainApplication();
		Map<String, List<VariableInstanceInfo>> cache = IWCacheManager2
		        .getInstance(iwma).getCache("bpmVariablesInfoCache", 1000,
		            true, false, 1209600); // 14 days
		return cache;
	}
	
	public void onApplicationEvent(final ApplicationEvent event) {
		if (event instanceof VariableCreatedEvent) {
			Thread importer = new Thread(new Runnable() {
				public void run() {
					VariableCreatedEvent varCreated = (VariableCreatedEvent) event;
					
					Map<String, Object> vars = varCreated.getVariables();
					if (vars == null) {
						return;
					}
					
					Long processInstanceId = varCreated.getProcessInstanceId();
					Map<String, List<VariableInstanceInfo>> cache = getVariablesCache();
					for (String name : vars.keySet()) {
						List<VariableInstanceInfo> cachedValues = cache
						        .get(name);
						if (cachedValues == null) {
							continue;
						}
						
						VariableInstanceInfo info = getEmptyVariable(name);
						if (info == null) {
							continue;
						}
						
						Object value = vars.get(name);
						if (value instanceof Serializable) {
							info.setProcessInstanceId(processInstanceId);
							info.setValue((Serializable) value);
							cachedValues.add(info);
						}
					}
				}
			});
			importer.start();
		}
	}
}