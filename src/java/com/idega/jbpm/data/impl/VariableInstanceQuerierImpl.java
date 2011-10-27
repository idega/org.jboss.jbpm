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
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.cache.IWCacheManager2;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
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
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.reflect.MethodInvoker;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariableInstanceQuerierImpl extends GenericDaoImpl implements VariableInstanceQuerier, ApplicationListener {
	
	private static final Logger LOGGER = Logger.getLogger(VariableInstanceQuerierImpl.class.getName());
	
	@Autowired
	private BPMContext bpmContext;
	
	private static final String VARIABLES_TABLE = "JBPM_VARIABLEINSTANCE",
								STANDARD_COLUMNS = "var.ID_ as varid, var.NAME_ as name, var.CLASS_ as type",
								FROM = " from " + VARIABLES_TABLE + " var",
								CLASS_CONDITION = " var.CLASS_ <> '" + VariableInstanceType.NULL.getTypeKeys().get(0) + "' ",
								NAME_CONDITION = " var.NAME_ is not null ",
								CONDITION = NAME_CONDITION + " and" + CLASS_CONDITION,
								VAR_DEFAULT_CONDITION = " and" + CONDITION,
								OTHER_VALUES = " var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.BYTEARRAYVALUE_ as bv, var.PROCESSINSTANCE_ as piid",
								MIRRROW_TABLE_ALIAS = "mirrow",
								PROC_INST_IDS_EXPRESSION = ":processInstanceIdsExpression";
	
	private static final int COLUMNS = 3,
							FULL_COLUMNS = COLUMNS + 6;
	
	private static final Random ID_GENERATOR = new Random();
	
	private List<String> cachedVariablesNames = new ArrayList<String>();
	
	private String getSelectPart(String columns) {
		return getSelectPart(columns, Boolean.FALSE);
	}
	
	private String getSelectPart(String columns, boolean distinct) {
		return "select ".concat(distinct ? "distinct " : CoreConstants.EMPTY).concat(columns);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName, false);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName, boolean full) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			LOGGER.warning("Process definition name is not provided");
			return null;
		}
		
		int columns = full ? FULL_COLUMNS : 2;
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns() : "var.NAME_ as name, var.CLASS_ as type";
			query = getQuery(getSelectPart(selectColumns, !full), FROM, " inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ = pi.ID_ ",
			    "inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ where ", CONDITION, " and pd.NAME_ = '", processDefinitionName, "'");
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process definition: " + processDefinitionName, e);
		}
		
		return getConverted(data, columns);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessDefinition(String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			LOGGER.warning("Process definition name is not provided");
			return null;
		}
		
		List<ProcessDefinitionVariablesBind> binds = getResultList(
		    ProcessDefinitionVariablesBind.QUERY_SELECT_BY_PROCESS_DEFINITION_NAMES,
		    ProcessDefinitionVariablesBind.class,
		    new Param(ProcessDefinitionVariablesBind.PARAM_PROC_DEF_NAMES,
		            Arrays.asList(processDefinitionName)));
		if (!ListUtil.isEmpty(binds))
			return getConverted(binds);
		
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName, true);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId, boolean mirrow) {
		return getVariablesByProcessInstanceId(processInstanceId, true, mirrow);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId, boolean full, boolean mirrow) {
		if (processInstanceId == null) {
			LOGGER.warning("Invalid ID of process instance");
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns(mirrow, false) : STANDARD_COLUMNS;
			int columns = full ? FULL_COLUMNS : COLUMNS;
			query = getQuery(getSelectPart(selectColumns), getFromClause(mirrow), " where var.PROCESSINSTANCE_ = ", String.valueOf(processInstanceId),
					VAR_DEFAULT_CONDITION, getMirrowTableCondition(mirrow));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process instance: " + processInstanceId, e);
		}
		
		return getConverted(data, full ? FULL_COLUMNS : COLUMNS);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, false, isDataMirrowed());
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, true, isDataMirrowed());
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names) {
		return getVariablesByProcessInstanceIdAndVariablesNames(procIds, names, true);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names,
			boolean checkTaskInstance) {
		return getVariablesByProcessesAndVariablesNames(procIds, null, names, checkTaskInstance, true);
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
		
		return getVariablesByProcessesAndVariablesNames(names, null, procIds, checkTaskInstance, addEmptyVars, mirrowData);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessesAndVariablesNames(List<String> variablesNames, List<String> procDefNames, Collection<Long> procIds,
			boolean checkTaskInstance, boolean addEmptyVars, boolean mirrowData) {
		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("Variable name(s) unknown");
		}
		
		Map<Integer, VariableInstanceInfo> vars = new HashMap<Integer, VariableInstanceInfo>();
		if (mirrowData) {
			List<String> notStringVariables = new ArrayList<String>();
			for (String name : variablesNames) {
				if (!name.startsWith(VariableInstanceType.STRING.getPrefix())) {
					notStringVariables.add(name);
				}
			}
			List<String> stringVariables = new ArrayList<String>(variablesNames);
			if (!ListUtil.isEmpty(notStringVariables)) {
				stringVariables.removeAll(notStringVariables);
			}
			
			Collection<VariableInstanceInfo> stringVars = getVariablesByProcessesAndVariablesNames(procIds, procDefNames, stringVariables, mirrowData, checkTaskInstance);
			Collection<VariableInstanceInfo> otherVars = getVariablesByProcessesAndVariablesNames(procIds, procDefNames, notStringVariables, false, checkTaskInstance);
		
			fillMapWithData(vars, variablesNames, stringVars);
			fillMapWithData(vars, variablesNames, otherVars);
		} else {
			Collection<VariableInstanceInfo> allVars = getVariablesByProcessesAndVariablesNames(procIds, procDefNames, variablesNames, mirrowData, checkTaskInstance);
			fillMapWithData(vars, variablesNames, allVars);
		}
		
		List<VariableInstanceInfo> variables = null;
		if (vars.size() > 0) {
			variables = new ArrayList<VariableInstanceInfo>();
			if (addEmptyVars) {
				for (int i = 0; i < variablesNames.size(); i++) {
					VariableInstanceInfo info = vars.get(i);
					if (info == null) {
						info = getEmptyVariable(variablesNames.get(i));
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
		return piId == null ? index : index * 100000000 + piId.intValue();
	}
	
	public List<String> getValuesByVariableFromMirrowedTable(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		List<Serializable[]> data = null;
		String query = "select distinct m." + BPMVariableData.COLUMN_VALUE + " from " + BPMVariableData.TABLE_NAME + " m, JBPM_VARIABLEINSTANCE v where v.NAME_ = '" + name +
			"' and v.ID_ = m." + BPMVariableData.COLUMN_VARIABLE_ID + " order by m." + BPMVariableData.COLUMN_VALUE;
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data))
			return null;
		
		List<String> results = new ArrayList<String>();
		for (Serializable[] value: data) {
			if (ArrayUtil.isEmpty(value))
				continue;
			
			results.add(value[0].toString());
		}
		return results;
	}
	
	public List<String> getValuesByVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;
		
		List<Serializable[]> data = null;
		String query = "select distinct v.stringvalue_ from JBPM_VARIABLEINSTANCE v where v.NAME_ = '" + name +
			"' order by v.stringvalue_";
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data))
			return null;
		
		List<String> results = new ArrayList<String>();
		for (Serializable[] value: data) {
			if (ArrayUtil.isEmpty(value) || value[0] == null)
				continue;
			
			results.add(value[0].toString());
		}
		return results;
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessesAndVariablesNames(Collection<Long> procInstIds, List<String> procDefNames, List<String> variablesNames,
			boolean mirrow,	boolean checkTaskInstance) {
		
		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("The names of variables are not provided!");
			return null;
		}
		if (ListUtil.isEmpty(procInstIds) && ListUtil.isEmpty(procDefNames)) {
			LOGGER.warning("Proc. inst. IDs or proc. def. names must be provided!");
			return null;
		}
		
		variablesNames = new ArrayList<String>(variablesNames);
		List<String> cachedVars = new ArrayList<String>();
		List<VariableInstanceInfo> allVariables = new ArrayList<VariableInstanceInfo>();
		for (String name: variablesNames) {
			List<VariableInstanceInfo> vars = getCachedVariables(name);
			if (ListUtil.isEmpty(vars))
				continue;
			
			cachedVars.add(name);
			if (!ListUtil.isEmpty(procInstIds)) {
				for (VariableInstanceInfo var: vars) {
					if (var == null)
						continue;
					
					Long procInstId = var.getProcessInstanceId();
					if (procInstId == null)
						continue;
					
					if (procInstIds.contains(procInstId))
						allVariables.add(var);
				}
			}
		}
		
		variablesNames.removeAll(cachedVars);
		if (ListUtil.isEmpty(variablesNames))
			return allVariables;
		
		boolean anyStringColumn = false;
		for (Iterator<String> namesIter = variablesNames.iterator(); (!anyStringColumn && namesIter.hasNext());) {
			anyStringColumn = namesIter.next().contains(VariableInstanceType.STRING.getPrefix());
		}
		
		Collection<VariableInstanceInfo> vars = null;
		
		boolean byProcInstIds = !ListUtil.isEmpty(procInstIds);
		
		String query = null;
		int columns = anyStringColumn ? FULL_COLUMNS : FULL_COLUMNS - 1;
		try {
			String varNamesIn = getQueryParameters("var.NAME_", variablesNames, true);
			query = getQuery(getSelectPart(anyStringColumn ? getFullColumns(mirrow, false) : getAllButStringColumn(), false),
					mirrow ? " from ".concat(BPMVariableData.TABLE_NAME).concat(" mirrow, ").concat(VARIABLES_TABLE).concat(" var ") : getFromClause(true, mirrow),
					checkTaskInstance ? ", jbpm_taskinstance task " : CoreConstants.EMPTY, 
					byProcInstIds ? CoreConstants.EMPTY :
									" inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ ",
					" where ", byProcInstIds ? PROC_INST_IDS_EXPRESSION : getQueryParameters("pd.name_", procDefNames, true),
					mirrow ? getMirrowTableCondition(true) : CoreConstants.EMPTY, " and ", varNamesIn,
					checkTaskInstance ? " and var.TASKINSTANCE_ = task.ID_ and task.END_ is not null " : CoreConstants.EMPTY, " and ", CLASS_CONDITION,
					" order by var.TASKINSTANCE_");
			vars = byProcInstIds ? getVariablesByProcessInstanceIds(null, query, columns, new ArrayList<Long>(procInstIds)) : getVariablesByQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables for process instance(s) : " + procInstIds + " and name(s): "
					+ variablesNames, e);
		}
		
		if (ListUtil.isEmpty(allVariables))
			return vars;
		
		if (!ListUtil.isEmpty(vars))
			allVariables.addAll(vars);
		return allVariables;
	}
	
	public boolean isVariableStored(String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, false);
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
	
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(String name, Serializable value) {
		return getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(name, value, null);
	}
	
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(String name, Serializable value, List<Long> procInstIds) {
		VariableInstanceInfo cachedVariable = getCachedVariable(name, value);
		boolean useCaching = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getBoolean("use_cached_proc_inst_ids", true);
		
		if (cachedVariable != null && cachedVariable.getProcessInstanceId() != null && useCaching) {
			return Arrays.asList(cachedVariable.getProcessInstanceId());
		}
		
		Collection<VariableInstanceInfo> variables = null;
		try {
			variables = getProcessVariablesByNameAndValue(name, Arrays.asList(value), null, procInstIds, true, false, isDataMirrowed());
		} catch (Exception e) {
			String message = "Error getting variable '" + name + "', by value '" + value + "' and proc. inst. IDs: " + procInstIds;
			LOGGER.log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}
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
	
	public Collection<VariableInstanceInfo> getVariablesByNameAndValue(String name, Serializable value) {
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, false);
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
	
	private boolean isValueEquivalent(VariableInstanceInfo variable, Serializable value) {
		if (value instanceof String && variable instanceof VariableStringInstance) {
			String searchValue = (String) value;
			String variableValue = ((VariableStringInstance) variable).getValue();
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
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, Serializable value, boolean selectProcessInstanceId) {
		return getProcessVariablesByNameAndValue(name, Arrays.asList(value), null, null, selectProcessInstanceId, false, isDataMirrowed());
	}
	
	private String getStringValueExpression(String value) {
		return value != null && isDataMirrowed() && value.length() > 255 ? " ".concat(getSubstring(value)).concat(" ") : value;
	}
	
	private String getColumnAndValueExpression(String columnName, Serializable value, boolean searchExpression, boolean stringColumn) {		
		StringBuilder expression = new StringBuilder();
		expression.append(searchExpression && stringColumn ? " lower(" : CoreConstants.EMPTY).append(columnName)
		.append(searchExpression && stringColumn ? ")" : CoreConstants.EMPTY)
		.append(stringColumn ? " like " : " = ").append(searchExpression && stringColumn ? "lower(" : CoreConstants.EMPTY).append(stringColumn ? "'" : CoreConstants.EMPTY)
		.append(searchExpression && stringColumn ? "%" : CoreConstants.EMPTY).append(stringColumn ? getStringValueExpression((String) value) : value)
		.append(stringColumn && searchExpression ? "%" : CoreConstants.EMPTY).append(stringColumn ? "'" : CoreConstants.EMPTY)
		.append(searchExpression && stringColumn ? ")" : CoreConstants.EMPTY);
		return expression.toString();
	}
	
	BPMContext getBpmContext() {
		if (bpmContext == null)
			ELUtil.getInstance().autowire(this);
		return bpmContext;
	}
	
	private Collection<VariableInstanceInfo> getVariablesByNameAndValuesAndProcessDefinitions(String name, List<String> procDefNames, Collection<?> values,
			List<Long> procInstIds) {
		if (StringUtil.isEmpty(name))
			return Collections.emptyList();
		
		int columns = COLUMNS + 2;
		boolean byProcInst = !ListUtil.isEmpty(procInstIds);
		String query = getQuery(
				getSelectPart(STANDARD_COLUMNS),
				", b.BYTES_, var.PROCESSINSTANCE_ ",
				getFromClause(true, false),
				" inner join JBPM_BYTEBLOCK b on var.BYTEARRAYVALUE_ = b.PROCESSFILE_ ",
					byProcInst ?
						CoreConstants.EMPTY :
						" inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ ",
				" where ",
					byProcInst ?
						PROC_INST_IDS_EXPRESSION :
						getQueryParameters("pd.name_", procDefNames, true).concat(" and var.name_ = '").concat(name).concat("'"),
				" and var.CLASS_ = 'B' order by var.TASKINSTANCE_"
		);
		Collection<VariableInstanceInfo> vars = byProcInst ?
				getVariablesByProcessInstanceIds(null, query, columns, procInstIds) :
				getVariablesByQuery(query, columns);
		if (ListUtil.isEmpty(vars)) {
			return Collections.emptyList();
		}
		
		if (name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix())) {
			return getResolvedVariables(vars, name, values);
		}
		
		List<String> addedVars = new ArrayList<String>();
		Collection<VariableInstanceInfo> uniqueVars = new ArrayList<VariableInstanceInfo>();
		for (VariableInstanceInfo var: vars) {
			String key = getKeyForVariable(var, false);
			if (addedVars.contains(key))
				continue;
			
			Object value = var.getValue();
			if (value instanceof Collection<?>) {
				Collection<?> realValue = (Collection<?>) value;
				if (!CollectionUtils.isEqualCollection(realValue, values)) {
					continue;
				}
				
				uniqueVars.add(var);
				addedVars.add(key);
			}
		}
		
		return uniqueVars;
	}
	
	private Collection<VariableInstanceInfo> getResolvedVariables(Collection<VariableInstanceInfo> vars, String varName, Collection<?> values) {
		MultipleSelectionVariablesResolver resolver = null;
		try {
			resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + varName);
		} catch (Exception e) {}
		Collection<AdvancedProperty> resolvedValues = resolver == null ? null : resolver.getBinaryVariablesValues(vars);
		if (ListUtil.isEmpty(resolvedValues))
			return null;
		
		List<String> resolvedIds = new ArrayList<String>();
		for (AdvancedProperty resolved: resolvedValues) {
			for (Object value: values) {
				if (value instanceof Serializable && resolved.getId().equals(value.toString()) && !resolvedIds.contains(value.toString())) {
					resolvedIds.add(value.toString());
				}
			}
		}
		
		List<String> addedVars = new ArrayList<String>();
		Collection<VariableInstanceInfo> resolvedVars = new ArrayList<VariableInstanceInfo>();
		for (String resolvedId: resolvedIds) {
			for (VariableInstanceInfo var: vars) {
				if (var.getValue().toString().indexOf(resolvedId) != -1) {
					String key = getKeyForVariable(var, true).concat(String.valueOf(var.getProcessInstanceId()));
					if (!addedVars.contains(key)) {
						resolvedVars.add(var);
						addedVars.add(key);
					}
				}
			}
		}
		return resolvedVars;
	}
	
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames) {
		return getProcessVariablesByNameAndValue(name, values, procDefNames, null, true, true, isDataMirrowed());
	}
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames, List<Long> procInstIds,
			boolean selectProcessInstanceId, boolean searchExpression, boolean mirrow) {
		if (StringUtil.isEmpty(name) || ListUtil.isEmpty(values)) {
			LOGGER.warning("Variable name (" + name + ") and/or values (" + values + ") are not provided");
			return Collections.emptyList();
		}
		
		boolean anyStringColumn = false;
		Serializable value = values.get(0);
		if (value == null) {
			LOGGER.warning("Value for variable '" + name + "' must be specified!");
			return Collections.emptyList();
		}
		VariableInstanceType type = null;
		String columnName = "var.";
		if (values instanceof Collection<?> && (name.startsWith(VariableInstanceType.LIST.getPrefix()) || name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
				name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix()))) {
			return getVariablesByNameAndValuesAndProcessDefinitions(name, procDefNames, values, procInstIds);
		} else if (value instanceof String) {
			anyStringColumn = true;
			columnName = getStringValueColumn(mirrow);
			columnName = mirrow ? columnName : getSubstring(columnName);
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
			LOGGER.warning("Unsupported type of value: " + value + ", " + value.getClass());
			return Collections.emptyList();
		}
		
		StringBuffer valuesToSelect = new StringBuffer();
		boolean stringColumn = columnName.indexOf("string") != -1;
		if (values.size() > 1) {
			//	Multiple values
			if (stringColumn) {
				valuesToSelect.append("(");
				for (Iterator<Serializable> valuesIter = values.iterator(); valuesIter.hasNext();) {
					valuesToSelect.append(getColumnAndValueExpression(columnName, valuesIter.next(), searchExpression, stringColumn));
					if (valuesIter.hasNext()) {
						valuesToSelect.append(" or ");
					}
				}
				valuesToSelect.append(")");
			} else {
				valuesToSelect.append(columnName).append(" in (");
				for (Iterator<Serializable> valuesIter = values.iterator(); valuesIter.hasNext();) {
					valuesToSelect.append(valuesIter.next());
					if (valuesIter.hasNext()) {
						valuesToSelect.append(", ");
					}
				}
				valuesToSelect.append(") ");
			}
		} else {
			//	Single value
			valuesToSelect.append(getColumnAndValueExpression(columnName, value, searchExpression, stringColumn));
		}
			
		String valuesClause = valuesToSelect.toString();
		if (!StringUtil.isEmpty(valuesClause)) {
			valuesClause = " and ".concat(valuesClause);
		}
		
		boolean byProcessInstances = !ListUtil.isEmpty(procInstIds);
		boolean byProcDefs = !ListUtil.isEmpty(procDefNames);
		
		String query = null;
		int columns = COLUMNS + (selectProcessInstanceId ? 2 : 1);
		List<String> parts = new ArrayList<String>();
		parts.addAll(
			Arrays.asList(
				getSelectPart(STANDARD_COLUMNS, false), ", ", columnName,
					selectProcessInstanceId || byProcessInstances ? ", var.PROCESSINSTANCE_ as piid" : CoreConstants.EMPTY,
				" from ", (mirrow ? BPMVariableData.TABLE_NAME.concat(" ").concat(MIRRROW_TABLE_ALIAS).concat(", ") : CoreConstants.EMPTY),
				" JBPM_VARIABLEINSTANCE var ",
					byProcessInstances ?
							CoreConstants.EMPTY :
							byProcDefs ?
									" inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ " :
									CoreConstants.EMPTY,
				" where var.NAME_ = '", name, "' ", valuesClause,
					byProcessInstances ? " and ".concat(PROC_INST_IDS_EXPRESSION) : byProcDefs ?
							" and ".concat(getQueryParameters("pd.name_", procDefNames, true)) :
							CoreConstants.EMPTY
		));
		if (type != null) {
			parts.addAll(Arrays.asList(" and ", getQueryParameters("var.CLASS_", type.getTypeKeys(), true)));
		}
		parts.add(anyStringColumn && mirrow ? getMirrowTableCondition() : CoreConstants.EMPTY);
		parts.add(" order by var.TASKINSTANCE_");
		query = getQuery(ArrayUtil.convertListToArray(parts));
		
		return byProcessInstances ? getVariablesByProcessInstanceIds(null, query, columns, procInstIds) : getVariablesByQuery(query, columns);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceIds(Collection<VariableInstanceInfo> vars, String query, int columns, List<Long> procInstIds) {
		if (vars == null) {
			vars = new ArrayList<VariableInstanceInfo>();
		}
		
		if (ListUtil.isEmpty(procInstIds)) {
			return vars;
		}
		
		List<Long> usedIds = null;
		if (procInstIds.size() > 1000) {
			usedIds = new ArrayList<Long>(procInstIds.subList(0, 1000));
			procInstIds = new ArrayList<Long>(procInstIds.subList(1000, procInstIds.size()));
		} else {
			usedIds = new ArrayList<Long>(procInstIds);
			procInstIds = null;
		}
		
		Collection<VariableInstanceInfo> foundedVars = getVariablesByQuery(StringHandler.replace(query, PROC_INST_IDS_EXPRESSION,
				getQueryParameters("var.PROCESSINSTANCE_", usedIds, false)), columns);
		if (foundedVars != null) {
			vars.addAll(foundedVars);
		}
		
		return getVariablesByProcessInstanceIds(vars, query, columns, procInstIds);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByQuery(String query, int columns) {
		List<Serializable[]> data = null;
		try {
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'", e);
		}
		return getConverted(data, columns);
	}
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNamesAndValues(Map<String, List<Serializable>> namesAndValues, List<String> variables, List<String> procDefNames,
			List<Long> procInstIds, boolean selectProcessInstanceId, Set<Long> cachedVariables, Map<String, Boolean> flexibleVariables) {
		if (namesAndValues == null) {
			LOGGER.warning("Variables names and values are not provided");
			return null;
		}
		
		List<VariableInstanceInfo> allVars = null;
		for (Map.Entry<String, List<Serializable>> entry: namesAndValues.entrySet()) {
			Boolean searchExpression = flexibleVariables == null ? Boolean.FALSE : flexibleVariables.get(entry.getKey());
			Collection<VariableInstanceInfo> vars = getProcessVariablesByNameAndValue(entry.getKey(), entry.getValue(), procDefNames, procInstIds, selectProcessInstanceId,
					searchExpression == null ? false : searchExpression, false);
			if (ListUtil.isEmpty(vars)) {
				return Collections.emptyList();	//	We want to keep AND condition
			}
			
			allVars = doFilter(allVars, vars, cachedVariables);
			procInstIds = new ArrayList<Long>();
			for (VariableInstanceInfo var: allVars) {
				if (!procInstIds.contains(var.getProcessInstanceId())) {
					procInstIds.add(var.getProcessInstanceId());
				}
			}
			procDefNames = null;
		}
		
		if (!namesAndValues.isEmpty() && ListUtil.isEmpty(allVars)) {
			//	If values for variables were provided, but nothing found - returning empty list
			return Collections.emptyList();
		}
		
		if (!ListUtil.isEmpty(variables)) {
			//	Selecting the rest of the variables for which values were not provided
			Collection<VariableInstanceInfo> varsByNames = getVariablesByProcessesAndVariablesNames(variables, procDefNames, procInstIds, false, false,
					ListUtil.isEmpty(procInstIds) ? isDataMirrowed() : false);
			allVars = doFilter(allVars, varsByNames, cachedVariables);
		}
		
		return allVars;
	}
	
	private List<VariableInstanceInfo> doFilter(Collection<VariableInstanceInfo> previousVars, Collection<VariableInstanceInfo> foundVars, Set<Long> cachedVariables) {
		List<VariableInstanceInfo> commonVars = new ArrayList<VariableInstanceInfo>();
		if (ListUtil.isEmpty(foundVars))
			return commonVars;
		
		previousVars = ListUtil.isEmpty(previousVars) ? foundVars : previousVars;
		
		String key1 = null;
		String key2 = null;
		List<String> addedVars = new ArrayList<String>();
		for (VariableInstanceInfo previousVar: previousVars) {
			long piId = previousVar.getProcessInstanceId().longValue();
			key1 = getKeyForVariable(previousVar, false);
			if (addedVars.contains(key1))
				continue;
			
			List<VariableInstanceInfo> foundVarsByProccesInstanceId = getFilteredVariablesByProcessInstanceId(piId, foundVars);
			if (ListUtil.isEmpty(foundVarsByProccesInstanceId)) {
				continue;	//	Nothing common found
			}
			
			for (VariableInstanceInfo foundVar: foundVarsByProccesInstanceId) {
				key2 = getKeyForVariable(foundVar, true);
				if (addedVars.contains(key2))
					continue;
				
				if (!ListUtil.isEmpty(cachedVariables) && !(cachedVariables.contains(piId)))
					continue;
				
				commonVars.add(previousVar);
				addedVars.add(key1);
				if (!addedVars.contains(key2) && !commonVars.contains(foundVar)) {
					commonVars.add(foundVar);
					addedVars.add(key2);
				}
			}
		}
		return commonVars;
	}
	
	private List<VariableInstanceInfo> getFilteredVariablesByProcessInstanceId(long processInstanceId, Collection<VariableInstanceInfo> vars) {
		List<VariableInstanceInfo> filtered = new ArrayList<VariableInstanceInfo>();
		for (VariableInstanceInfo var: vars) {
			if (processInstanceId == var.getProcessInstanceId().longValue()) {
				filtered.add(var);
			}
		}
		return filtered;
	}
	
	private String getKeyForVariable(VariableInstanceInfo var, boolean useValue) {
		StringBuilder key = new StringBuilder(var.getName()).append(var.getProcessInstanceId());
		if (useValue)
			key.append(var.getValue());
		return key.toString();
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
				
				int piIdIndex = numberOfColumns == FULL_COLUMNS ? numberOfColumns - 1 : dataSet.length - 1;
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
					variable = id instanceof Number ? new VariableStringInstance(id.longValue(), name, value) : new VariableStringInstance(name, value);
				} else if ((value instanceof Long || value instanceof Number) && VariableInstanceType.LONG.getTypeKeys().contains(type)) {
					variable = new VariableLongInstance(name, ((Number) value).longValue());
				} else if ((value instanceof Double || value instanceof Number) && VariableInstanceType.DOUBLE.getTypeKeys().contains(type)) {
					variable = new VariableDoubleInstance(name, ((Number) value).doubleValue());
				} else if (value instanceof Timestamp && VariableInstanceType.DATE.getTypeKeys().contains(type)) {
					variable = new VariableDateInstance(name, (Timestamp) value);
				} else if (value instanceof Date && VariableInstanceType.DATE.getTypeKeys().contains(type)) {
					variable = new VariableDateInstance(name, new Timestamp(((Date) value).getTime()));
				} else if (value instanceof Byte[] || VariableInstanceType.BYTE_ARRAY.getTypeKeys().contains(type) || VariableInstanceType.OBJ_LIST.getTypeKeys().contains(type)) {
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
			if (target == null)
				return null;
			
			return (T) MethodInvoker.getInstance().invokeMethodWithNoParameters(target, method);
		} catch (Exception e) {
			String message = "Error invoking method " + method + " on object: " + target + " class: " + target.getClass();
			LOGGER.log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}
		return null;
	}
	
	private String getFullColumns() {
		return getFullColumns(isDataMirrowed(), false);
	}

	private String getFullColumns(boolean mirrow, boolean substring) {
		String columns = STANDARD_COLUMNS.concat(", ");
		String valueColumn = getStringValueColumn(mirrow);
		valueColumn = mirrow ? valueColumn : substring ? getSubstring(valueColumn) : valueColumn;
		return columns.concat(valueColumn).concat(" as sv, ").concat(OTHER_VALUES);
	}
	
	private String getAllButStringColumn() {
		return STANDARD_COLUMNS.concat(", ").concat(OTHER_VALUES);
	}
	
	private String getSubstring(String column) {
		return "substr(".concat(column).concat(", 1, 255)");
	}
	
	private String getQueryParameters(String columnName, Collection<? extends Serializable> values, boolean isString) {
		return getQueryParameters(columnName, values, Boolean.FALSE, isString);
	}
	
	private String getQueryParameters(String columnName, Collection<? extends Serializable> values, boolean notEquals, boolean isString) {
		synchronized (values) {
			StringBuilder params = new StringBuilder();
			if (values.size() == 1) {
				params.append(StringUtil.isEmpty(columnName) ? CoreConstants.SPACE : CoreConstants.SPACE.concat(columnName));
				params.append(notEquals ? " <> " : " = ");
				Serializable value = values.iterator().next();
				if (isString) {
					params.append(CoreConstants.QOUTE_SINGLE_MARK);
				}
				params.append(String.valueOf(value));
				if (isString) {
					params.append(CoreConstants.QOUTE_SINGLE_MARK);
				}
			} else {
				params.append(StringUtil.isEmpty(columnName) ? " (" : CoreConstants.SPACE.concat(columnName).concat(notEquals ? " not " : CoreConstants.EMPTY).concat(" in ("));
				for (Iterator<? extends Serializable> iter = values.iterator(); iter.hasNext();) {
					Serializable value = iter.next();
					if (isString) {
						params.append(CoreConstants.QOUTE_SINGLE_MARK);
					}
					params.append(value.toString());
					if (isString) {
						params.append(CoreConstants.QOUTE_SINGLE_MARK);
					}
					
					if (iter.hasNext()) {
						params.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
					}
				}
				params.append(CoreConstants.BRACKET_RIGHT);
			}
			
			params.append(CoreConstants.SPACE);
			return params.toString();
		}
	}
	
	public Collection<VariableInstanceInfo> getVariablesByNames(List<String> names) {
		if (ListUtil.isEmpty(names)) {
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			query = getQuery(getSelectPart(getFullColumns()), getFromClause(), " where", getQueryParameters("var.NAME_", names, true), VAR_DEFAULT_CONDITION,
					getMirrowTableCondition());
			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables by names: " + names, e);
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
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("jbpm_variables_mirrowed", Boolean.TRUE);
	}
	
	private String getStringValueColumn(boolean mirrow) {
		return mirrow ? MIRRROW_TABLE_ALIAS.concat(".stringvalue") : "var.stringvalue_";
	}
	
	private String getMirrowTableCondition() {
		return getMirrowTableCondition(true);
	}
	
	private String getMirrowTableCondition(boolean full) {
		return (full && isDataMirrowed()) ? " and var.ID_ = " + MIRRROW_TABLE_ALIAS + ".variable_id " : CoreConstants.EMPTY;
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
			from = from.concat(", ").concat(BPMVariableData.TABLE_NAME).concat(" ").concat(MIRRROW_TABLE_ALIAS);
		}
		return from;
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds) {
		return getFullVariablesByProcessInstanceIdsNaiveWay(processInstanceIds, null);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}
		
		int columns = COLUMNS + 2;
		List<Serializable[]> data = null;
		String query = getQuery("select ", STANDARD_COLUMNS, ", ", getSubstring("var.STRINGVALUE_"), " as sv, var.PROCESSINSTANCE_ ", FROM, " where ",
		    getQueryParameters("var.PROCESSINSTANCE_", processInstanceIds, false), " and ", NAME_CONDITION + " and ",
		    getQueryParameters("var.CLASS_", VariableInstanceType.STRING.getTypeKeys(), true));
		if (!ListUtil.isEmpty(existingVars)) {
			query = query.concat(" and ").concat(getQueryParameters("var.ID_", existingVars, true, false));
		}
		try {
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables by process instance IDs. Query: " + query, e);
		}
		
		return getConverted(data, columns);
	}
	
	public void loadVariables(List<String> variablesNames) {
		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("No variables provided!");
			return;
		}
		
		for (String name: variablesNames) {
			if (!cachedVariablesNames.contains(name)) {
				cachedVariablesNames.add(name);
			}
		}
		
		Collection<VariableInstanceInfo> vars = getVariablesByNames(variablesNames);
		if (vars == null) {
			return;
		}
		
		Map<String, List<VariableInstanceInfo>> cache = getVariablesCache();
		for (VariableInstanceInfo info : vars) {
			String variableName = info.getName();
			if (StringUtil.isEmpty(variableName)) {
				continue;
			}
			Serializable value = info.getValue();
			if (value == null) {
				continue;
			}
			
			List<VariableInstanceInfo> variables = cache.get(variableName);
			if (variables == null) {
				variables = new ArrayList<VariableInstanceInfo>();
				cache.put(variableName, variables);
			}
			variables.add(info);
		}
	}
	
	private List<VariableInstanceInfo> getCachedVariables(String name) {
		return getCachedVariables(name, null, false, true);
	}
	
	private List<VariableInstanceInfo> getCachedVariables(String name, Serializable value, boolean approximate, boolean allVariables) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}
		
		Map<String, List<VariableInstanceInfo>> cache = getVariablesCache();
		List<VariableInstanceInfo> vars = cache.get(name);
		if (ListUtil.isEmpty(vars) || value == null)
			return vars;
		
		List<VariableInstanceInfo> allVars = new ArrayList<VariableInstanceInfo>();
		
		VariableInstanceInfo variable = null;
		List<String> addedVars = new ArrayList<String>();
		for (Iterator<VariableInstanceInfo> varsIterator = vars.iterator(); (varsIterator.hasNext() && (allVariables ? Boolean.TRUE : variable == null));) {
			VariableInstanceInfo var = varsIterator.next();
			
			Serializable varValue = var.getValue();
			if (varValue == null)
				continue;
			
			String key = getKeyForVariable(var, false);
			if (addedVars.contains(key))
				continue;
			
			boolean equal = false;
			if (var instanceof VariableStringInstance) {
				String realValue = value.toString();
				String realVarValue = (String) varValue;
				
				if (approximate) {
					realValue = realValue.toLowerCase();
					realVarValue = realVarValue.toLowerCase();
					equal = realVarValue.indexOf(realValue) != -1;
				} else {
					equal = value.toString().equals(varValue);
				}
			} else if (var instanceof VariableDateInstance && ((Date) varValue).equals(value)) {
				variable = var;
			}
				
			variable = equal ? var : null;
			if (variable != null) {
				allVars.add(variable);
				addedVars.add(key);
			}
		}
		
		return allVars;
	}
	
	private VariableInstanceInfo getCachedVariable(String name, Serializable value) {
		List<VariableInstanceInfo> cached = getCachedVariables(name, value, false, false);
		return ListUtil.isEmpty(cached) ? null : cached.iterator().next();
	}
	
	private Map<String, List<VariableInstanceInfo>> getVariablesCache() {
		IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
		Map<String, List<VariableInstanceInfo>> cache = IWCacheManager2.getInstance(iwma).getCache("bpmVariablesInfoCache", 100000, true, false, 8640000, 8640000,
				false); // 100 days
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
						if (!cachedVariablesNames.contains(name)) {
							continue;
						}
						
						List<VariableInstanceInfo> cachedValues = cache.get(name);
						if (cachedValues == null) {
							cachedValues = new ArrayList<VariableInstanceInfo>();
							cache.put(name, cachedValues);
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

	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesByProcesses(Map<String, List<Serializable>> activeVariables, List<String> variables,
			List<String> procDefNames, List<Long> procInstIds, Map<String, Boolean> flexibleVariables) {
		List<String> variablesToQuery = new ArrayList<String>();
		
		//	Determining cached variables
		Map<String, List<VariableInstanceInfo>> cachedVariables = new HashMap<String, List<VariableInstanceInfo>>();
		if (activeVariables != null && !activeVariables.isEmpty()) {
			for (String name: activeVariables.keySet()) {
				for (Serializable value: activeVariables.get(name)) {
					List<VariableInstanceInfo> info = getCachedVariables(name, value, flexibleVariables == null ? false : flexibleVariables.get(name) == null ? false : flexibleVariables.get(name), true);
					if (info == null) {
						continue;
					}
				
					cachedVariables.put(name, info);
				}
				
				List<VariableInstanceInfo> cached = cachedVariables.get(name);
				if (cached == null || cached.size() < activeVariables.get(name).size()) {
					if (!variablesToQuery.contains(name)) {
						variablesToQuery.add(name);
					}
					cachedVariables.remove(name);
				}
			}
		}
		
		Map<Long, Map<String, VariableInstanceInfo>> results = new HashMap<Long, Map<String, VariableInstanceInfo>>();
		if (!cachedVariables.isEmpty()) {
			//	Grouping variables by processes
			for (Entry<String, List<VariableInstanceInfo>> entry: cachedVariables.entrySet()) {
				for (VariableInstanceInfo varInfo: entry.getValue()) {
					Long piId = varInfo.getProcessInstanceId();
					Map<String, VariableInstanceInfo> processVariables = results.get(piId);
					if (processVariables == null) {
						processVariables = new HashMap<String, VariableInstanceInfo>();
						results.put(piId, processVariables);
					}
					processVariables.put(varInfo.getName(), varInfo);
				}
			}
			
			//	Must query by process instance IDs that were found by cached variables
			procInstIds = new ArrayList<Long>(results.keySet());
		}
		
		if (ListUtil.isEmpty(variablesToQuery) && ListUtil.isEmpty(variables)) {
			//	Everything was found in cache or nothing else needed to query
			return results;
		}
		
		Map<String, List<Serializable>> variablesAndValuesToQuery = new HashMap<String, List<Serializable>>();
		for (String variableToQuery: variablesToQuery) {
			variablesAndValuesToQuery.put(variableToQuery, activeVariables.get(variableToQuery));
		}
		
		Collection<VariableInstanceInfo> info = null;
		//	Querying the DB for variables
		info = getProcessVariablesByNamesAndValues(variablesAndValuesToQuery, variables, ListUtil.isEmpty(procInstIds) ? procDefNames : null, procInstIds, true,
				results.keySet(), flexibleVariables);
		
		if (ListUtil.isEmpty(info)) {
			return null;
		}
		
		//	Combining the cached results and the queried results
		for (VariableInstanceInfo varInfo: info) {
			Long piId = varInfo.getProcessInstanceId();
			Map<String, VariableInstanceInfo> processVariables = results.get(piId);
			if (processVariables == null) {
				processVariables = new HashMap<String, VariableInstanceInfo>();
				results.put(piId, processVariables);
			}
			processVariables.put(varInfo.getName(), varInfo);
		}
		
		return results;
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIds(Collection<Long> procInstIds) {
		if (ListUtil.isEmpty(procInstIds))
			return null;
		
		String query = getQuery(getSelectPart(STANDARD_COLUMNS), getFromClause(false, false), " where ", PROC_INST_IDS_EXPRESSION, " group by var.name_");
		return getVariablesByProcessInstanceIds(null, query, COLUMNS, new ArrayList<Long>(procInstIds));
	}

	public List<VariableInstanceInfo> getVariablesByNameAndTaskInstance(Collection<String> names, Long tiId) {
		if (ListUtil.isEmpty(names) || tiId == null)
			return null;
		
		String query = getQuery(getSelectPart(STANDARD_COLUMNS), CoreConstants.COMMA, OTHER_VALUES, getFromClause(true, false), " where var.taskinstance_ = ",
				String.valueOf(tiId), " and ", getQueryParameters("var.name_", names, Boolean.TRUE));
		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, 8);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(results))
			return null;
		
		Collection<VariableInstanceInfo> vars = getConverted(results, 8);
		if (ListUtil.isEmpty(vars))
			return null;
		return new ArrayList<VariableInstanceInfo>(vars);
	}

	public Map<Long, List<VariableInstanceInfo>> getGroupedVariables(Collection<VariableInstanceInfo> variables) {
		if (ListUtil.isEmpty(variables))
			return null;
		
		Map<Long, List<VariableInstanceInfo>> grouped = new HashMap<Long, List<VariableInstanceInfo>>();
		for (VariableInstanceInfo var: variables) {
			Long piId = var.getProcessInstanceId();
			if (piId == null)
				continue;
			
			List<VariableInstanceInfo> vars = grouped.get(piId);
			if (vars == null) {
				vars = new ArrayList<VariableInstanceInfo>();
				grouped.put(piId, vars);
			}
			vars.add(var);
		}
		
		return grouped;
	}
}