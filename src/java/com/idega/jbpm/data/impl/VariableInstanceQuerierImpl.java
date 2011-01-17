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
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.VariableInstance;
import org.jbpm.context.exe.variableinstance.ByteArrayInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.cache.IWCacheManager2;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
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
import com.idega.util.expression.ELUtil;
import com.idega.util.reflect.MethodInvoker;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariableInstanceQuerierImpl extends GenericDaoImpl implements VariableInstanceQuerier, ApplicationListener {
	
	private static final Logger LOGGER = Logger.getLogger(VariableInstanceQuerierImpl.class.getName());
	
	@Autowired
	private BPMContext bpmContext;
	
	private static final String VARIABLES_TABLE = "JBPM_VARIABLEINSTANCE";
	private static final String STANDARD_COLUMNS = "var.ID_ as varid, var.NAME_ as name, var.CLASS_ as type";
	private static final String FROM = " from " + VARIABLES_TABLE + " var";
	private static final String CLASS_CONDITION = " var.CLASS_ <> '" + VariableInstanceType.NULL.getTypeKeys().get(0) + "' ";
	private static final String NAME_CONDITION = " var.NAME_ is not null ";
	private static final String CONDITION = NAME_CONDITION + " and" + CLASS_CONDITION;
	private static final String VAR_DEFAULT_CONDITION = " and" + CONDITION;
	private static final String NOT_STRING_VALUES = " var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.PROCESSINSTANCE_ as piid";
	
	private static final String MIRRROW_TABLE_ALIAS = "mirrow";
	
	private static final int COLUMNS = 3;
	private static final int FULL_COLUMNS = COLUMNS + 5;
	
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
		
		return getConverted(binds);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName, true);
	}
	
	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId, boolean full) {
		if (processInstanceId == null) {
			LOGGER.warning("Invalid ID of process instance");
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns() : STANDARD_COLUMNS;
			int columns = full ? FULL_COLUMNS : COLUMNS;
			query = getQuery(getSelectPart(selectColumns), getFromClause(full), " where var.PROCESSINSTANCE_ = ", String.valueOf(processInstanceId),
					VAR_DEFAULT_CONDITION, getMirrowTableCondition(full));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process instance: " + processInstanceId, e);
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
		return getVariablesByProcessInstanceIdAndVariablesNames(procIds, names, true);
	}
	
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names,
			boolean checkTaskInstance) {
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
			if (!name.startsWith(VariableInstanceType.STRING.getPrefix())) {
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
		return piId == null ? index : index * 100000000 + piId.intValue();
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
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables for process instance(s) : " + procIds + " and name(s): "
					+ names, e);
		}
		
		return getConverted(data, columns);
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
		VariableInstanceInfo cachedVariable = getCachedVariable(name, value);
		if (cachedVariable != null && cachedVariable.getProcessInstanceId() != null) {
			return Arrays.asList(cachedVariable.getProcessInstanceId());
		}
		
		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, true);
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
		return getProcessVariablesByNameAndValue(name, Arrays.asList(value), null, null, selectProcessInstanceId, false);
	}
	
	private String getStringValueExpression(String value) {
		return value != null && isDataMirrowed() && value.length() > 255 ? " substring(".concat(value).concat(", 1, 255) ") : value;
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
	
	@Transactional(readOnly=true)
	private Collection<VariableInstanceInfo> getVariablesByNameProcessDefinitions(final String name, final List<String> procDefNames, final Collection<?> values) {
		try {
			return getBpmContext().execute(new JbpmCallback() {
				public Object doInJbpm(JbpmContext context) throws JbpmException {
					List<VariableInstance> vars = getResultListByInlineQuery("select distinct v from " + VariableInstance.class.getName() + " v, " +
							ProcessInstance.class.getName() + " pi, " + ProcessDefinition.class.getName() + " pd where pd.name = :procDefNames and pi.processDefinition = pd.id " +
							"and v.processInstance = pi.id and v.name = :varName group by v.processInstance",
							VariableInstance.class,
							new Param("procDefNames", procDefNames),
							new Param("varName", name)
					);
					if (ListUtil.isEmpty(vars)) {
						return Collections.emptyList();
					}
					
					Collection<VariableInstanceInfo> variables = new ArrayList<VariableInstanceInfo>();
					for (VariableInstance var: vars) {
						Object value = var.getValue();
						if (value instanceof Collection<?>) {
							Collection<?> realValue = (Collection<?>) value;
							if (CollectionUtils.isEqualCollection(realValue, values)) {
								VariableInstanceInfo v = getConverted(var, (Serializable) realValue);
								if (v != null && !variables.contains(v)) {
									variables.add(v);
								}
							}
						}
					}
					return variables;
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables (" + VariableInstance.class.getName() + ") by name: " + name + " and process definitions: " + procDefNames, e);
		}
		return null;
	}
	
	@Transactional(readOnly=true)
	private VariableInstanceInfo getConverted(VariableInstance variable, Serializable value) {
		VariableInstanceInfo v = null;
		Class<? extends VariableInstance> theClass = variable.getClass();
		if (theClass.getName().equals(ByteArrayInstance.class.getName())) {
			v = new VariableByteArrayInstance(variable.getName(), value);
		}
		if (v != null) {
			v.setProcessInstanceId(variable.getProcessInstance().getId());
			v.setId(ID_GENERATOR.nextLong());
		}
		return v;
	}
	
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames) {
		return getProcessVariablesByNameAndValue(name, values, procDefNames, null, true, true);
	}
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames, List<Long> procInstIds,
			boolean selectProcessInstanceId, boolean searchExpression) {
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
		if (values instanceof Collection<?> && name.startsWith(VariableInstanceType.LIST.getPrefix())) {
			//	List value - can query by values using Hibernate query only
			return getVariablesByNameProcessDefinitions(name, procDefNames, values);
		} else if (value instanceof String) {
			anyStringColumn = true;
			columnName = columnName.concat(getStringValueColumn());
			columnName = getStringValueColumn();
			columnName = isDataMirrowed() ? columnName : getSubstring(columnName);
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
		
		boolean byProcessDefinitions = !ListUtil.isEmpty(procDefNames);
		boolean byProcessInstances = !ListUtil.isEmpty(procInstIds);
		
		String query = null;
		int columns = COLUMNS + (selectProcessInstanceId ? 2 : 1);
		List<Serializable[]> data = null;
		try {
			List<String> parts = new ArrayList<String>();
			parts.addAll(Arrays.asList(
					getSelectPart(STANDARD_COLUMNS, false), ", ", columnName,
						selectProcessInstanceId || byProcessDefinitions || byProcessInstances ? ", var.PROCESSINSTANCE_ as piid" : CoreConstants.EMPTY,
					" from ", (isDataMirrowed() ? BPMVariableData.TABLE_NAME.concat(" ").concat(MIRRROW_TABLE_ALIAS).concat(", ") : CoreConstants.EMPTY),
					" JBPM_VARIABLEINSTANCE var ",
						byProcessDefinitions ?
								" inner join JBPM_PROCESSINSTANCE pi on var.PROCESSINSTANCE_ = pi.ID_ inner join JBPM_PROCESSDEFINITION pd on pi.PROCESSDEFINITION_ = pd.ID_ " :
								CoreConstants.EMPTY,
					" where var.NAME_ = '", name, "' ", valuesClause,
						byProcessDefinitions ? " and ".concat(getQueryParameters("pd.name_", procDefNames)) : CoreConstants.EMPTY,
						byProcessInstances ? " and ".concat(getQueryParameters("var.PROCESSINSTANCE_", procInstIds)) : CoreConstants.EMPTY
			));
			if (type != null) {
				parts.addAll(Arrays.asList(" and ", getQueryParameters("var.CLASS_", type.getTypeKeys())));
			}
			parts.add(anyStringColumn ? getMirrowTableCondition() : CoreConstants.EMPTY);
			query = getQuery(ArrayUtil.convertListToArray(parts));
			
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables by name='" + name + "' and value='" + value + "'", e);
		}
		
		return getConverted(data, columns);
	}
	
	private Collection<VariableInstanceInfo> getProcessVariablesByNamesAndValues(Map<String, List<Serializable>> namesAndValues, List<String> procDefNames, List<Long> procInstIds,
			boolean selectProcessInstanceId, Set<Long> cachedVariables) {
		if (namesAndValues == null || namesAndValues.isEmpty()) {
			LOGGER.warning("Variables names and values are not provided");
			return null;
		}
		
		List<VariableInstanceInfo> allVars = new ArrayList<VariableInstanceInfo>();
		for (Map.Entry<String, List<Serializable>> entry: namesAndValues.entrySet()) {
			Collection<VariableInstanceInfo> vars = getProcessVariablesByNameAndValue(entry.getKey(), entry.getValue(), procDefNames, procInstIds, selectProcessInstanceId, true);
			if (ListUtil.isEmpty(vars)) {
				return Collections.emptyList();	//	We want to keep AND condition
			}
			
			if (allVars.size() == 0) {
				allVars.addAll(vars);
			} else {
				allVars = doFilter(allVars, vars, cachedVariables);
			}
		}
		
		return allVars;
	}
	
	private List<VariableInstanceInfo> doFilter(List<VariableInstanceInfo> previousVars, Collection<VariableInstanceInfo> foundedVars, Set<Long> cachedVariables) {
		List<VariableInstanceInfo> commonVars = new ArrayList<VariableInstanceInfo>();
		for (VariableInstanceInfo previousVar: previousVars) {
			long piId = previousVar.getProcessInstanceId().longValue();
			for (VariableInstanceInfo foundedVar: foundedVars) {
				if (piId == foundedVar.getProcessInstanceId().longValue()) {
					boolean addVars = true;
					if (!ListUtil.isEmpty(cachedVariables) && !(cachedVariables.contains(piId))) {
						addVars = false;
					}
					if (addVars) {
						commonVars.add(previousVar);
						commonVars.add(foundedVar);
					}
				}
			}
		}
		return commonVars;
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
			return (T) MethodInvoker.getInstance().invokeMethodWithNoParameters(target, method);
		} catch (Exception e) {
			String message = "Error invoking method " + method + " on object: " + target;
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
	
	private String getQueryParameters(String columnName, Collection<? extends Serializable> values) {
		return getQueryParameters(columnName, values, Boolean.FALSE, values.iterator().next() instanceof String);
	}
	
	private String getQueryParameters(String columnName, Collection<? extends Serializable> values, boolean notEquals, boolean isString) {
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
	
	public Collection<VariableInstanceInfo> getVariablesByNames(List<String> names) {
		if (ListUtil.isEmpty(names)) {
			return null;
		}
		
		String query = null;
		List<Serializable[]> data = null;
		try {
			query = getQuery(getSelectPart(getFullColumns()), getFromClause(), " where", getQueryParameters("var.NAME_", names), VAR_DEFAULT_CONDITION,
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
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(
	        List<Long> processInstanceIds) {
		return getFullVariablesByProcessInstanceIdsNaiveWay(processInstanceIds,
		    null);
	}
	
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}
		
		int columns = COLUMNS + 2;
		List<Serializable[]> data = null;
		String query = getQuery("select ", STANDARD_COLUMNS, ", ", getSubstring("var.STRINGVALUE_"), " as sv, var.PROCESSINSTANCE_ ", FROM, " where ",
		    getQueryParameters("var.PROCESSINSTANCE_", processInstanceIds), " and ", NAME_CONDITION + " and ",
		    getQueryParameters("var.CLASS_", VariableInstanceType.STRING.getTypeKeys()));
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
	
	private List<VariableInstanceInfo> getCachedVariables(String name, Serializable value) {
		return getCachedVariables(name, value, true);
	}
	
	private List<VariableInstanceInfo> getCachedVariables(String name, Serializable value, boolean approximate) {
		if (StringUtil.isEmpty(name) || value == null) {
			return null;
		}
		
		Map<String, List<VariableInstanceInfo>> cache = getVariablesCache();
		List<VariableInstanceInfo> vars = cache.get(name);
		if (ListUtil.isEmpty(vars)) {
			return null;
		}
		
		List<VariableInstanceInfo> allVars = new ArrayList<VariableInstanceInfo>();
		
		VariableInstanceInfo variable = null;
		for (Iterator<VariableInstanceInfo> varsIterator = vars.iterator(); (varsIterator.hasNext() && approximate ? Boolean.TRUE : variable == null);) {
			VariableInstanceInfo var = varsIterator.next();
			
			Serializable varValue = var.getValue();
			if (varValue == null) {
				continue;
			}
			
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
			if (variable != null && !allVars.contains(variable)) {
				allVars.add(variable);
			}
		}
		
		return allVars;
	}
	
	private VariableInstanceInfo getCachedVariable(String name, Serializable value) {
		List<VariableInstanceInfo> cached = getCachedVariables(name, value, false);
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

	@Override
	public Map<Long, List<VariableInstanceInfo>> getVariablesByNamesAndValuesByProcesses(Map<String, List<Serializable>> variables, List<String> procDefNames,
			List<Long> procInstIds) {
		List<String> variablesToQuery = new ArrayList<String>();
		
		//	Determining cached variables
		Map<String, List<VariableInstanceInfo>> cachedVariables = new HashMap<String, List<VariableInstanceInfo>>();
		for (String name: variables.keySet()) {
			for (Serializable value: variables.get(name)) {
				List<VariableInstanceInfo> info = getCachedVariables(name, value);
				if (info == null) {
					continue;
				}
			
				cachedVariables.put(name, info);
			}
			
			List<VariableInstanceInfo> cached = cachedVariables.get(name);
			if (cached == null || cached.size() < variables.get(name).size()) {
				if (!variablesToQuery.contains(name)) {
					variablesToQuery.add(name);
				}
				cachedVariables.remove(name);
			}
		}
		
		Map<Long, List<VariableInstanceInfo>> results = new HashMap<Long, List<VariableInstanceInfo>>();
		if (!cachedVariables.isEmpty()) {
			//	Grouping variables by processes
			for (Entry<String, List<VariableInstanceInfo>> entry: cachedVariables.entrySet()) {
				for (VariableInstanceInfo varInfo: entry.getValue()) {
					Long piId = varInfo.getProcessInstanceId();
					List<VariableInstanceInfo> processVariables = results.get(piId);
					if (processVariables == null) {
						processVariables = new ArrayList<VariableInstanceInfo>();
						results.put(piId, processVariables);
					}
					processVariables.add(varInfo);
				}
			}
		}
		
		if (ListUtil.isEmpty(variablesToQuery)) {
			//	Everything was found in cache
			return results;
		}
		
		Map<String, List<Serializable>> variablesAndValuesToQuery = new HashMap<String, List<Serializable>>();
		for (String variableToQuery: variablesToQuery) {
			variablesAndValuesToQuery.put(variableToQuery, variables.get(variableToQuery));
		}
		Collection<VariableInstanceInfo> info = null;
		if (!variablesAndValuesToQuery.isEmpty()) {
			//	Querying the DB for variables
			boolean byProcDef = !ListUtil.isEmpty(procDefNames) && procInstIds != null && procInstIds.size() > 1000;
			info = getProcessVariablesByNamesAndValues(variablesAndValuesToQuery, procDefNames, byProcDef ? null : procInstIds, true, results.keySet());
			if (ListUtil.isEmpty(info)) {
				return null;
			}
		}
		
		//	Combining the cached results and the queried results
		for (VariableInstanceInfo varInfo: info) {
			Long piId = varInfo.getProcessInstanceId();
			List<VariableInstanceInfo> processVariables = results.get(piId);
			if (processVariables == null) {
				processVariables = new ArrayList<VariableInstanceInfo>();
				results.put(piId, processVariables);
			}
			processVariables.add(varInfo);
		}
		
		return results;
	}
}