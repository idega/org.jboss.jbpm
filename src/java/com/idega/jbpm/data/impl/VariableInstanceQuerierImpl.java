package com.idega.jbpm.data.impl;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Termination;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.cache.IWCacheManager2;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.BPMProcessVariable;
import com.idega.jbpm.bean.VariableByteArrayInstance;
import com.idega.jbpm.bean.VariableDateInstance;
import com.idega.jbpm.bean.VariableDefaultInstance;
import com.idega.jbpm.bean.VariableDoubleInstance;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceInfoComparator;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.bean.VariableLongInstance;
import com.idega.jbpm.bean.VariableQuerierData;
import com.idega.jbpm.bean.VariableStringInstance;
import com.idega.jbpm.data.AutoloadedProcessDefinition;
import com.idega.jbpm.data.BPMVariableData;
import com.idega.jbpm.data.ProcessDefinitionVariablesBind;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.event.TaskInstanceSubmitted;
import com.idega.jbpm.event.VariableCreatedEvent;
import com.idega.jbpm.search.bridge.VariableDateInstanceBridge;
import com.idega.jbpm.variables.MultipleSelectionVariablesResolver;
import com.idega.presentation.ui.handlers.IWDatePickerHandler;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.reflect.MethodInvoker;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class VariableInstanceQuerierImpl extends GenericDaoImpl implements VariableInstanceQuerier, ApplicationListener<ApplicationEvent> {

	private static final Logger LOGGER = Logger.getLogger(VariableInstanceQuerierImpl.class.getName());

	@Autowired
	private BPMContext bpmContext;

	@Autowired
	private BPMDAO bpmDAO;

	private static final String VARIABLES_TABLE = "JBPM_VARIABLEINSTANCE",
								STANDARD_COLUMNS = "var.ID_ as varid, var.NAME_ as name, var.CLASS_ as type",
								FROM = " from " + VARIABLES_TABLE + " var",
								CLASS_CONDITION = " var.CLASS_ <> '" + VariableInstanceType.NULL.getTypeKeys().get(0) + "' ",
								NAME_CONDITION = " var.NAME_ is not null ",
								CONDITION = NAME_CONDITION + " and" + CLASS_CONDITION,
								VAR_DEFAULT_CONDITION = " and" + CONDITION,
								OTHER_VALUES = " var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.BYTEARRAYVALUE_ as bv," +
										" var.PROCESSINSTANCE_ as piid",
								MIRRROW_TABLE_ALIAS = "mirrow",
								PROC_INST_IDS_EXPRESSION = ":processInstanceIdsExpression";

	private static final int COLUMNS = 3,
							FULL_COLUMNS = COLUMNS + 6;

	private static final Random ID_GENERATOR = new Random();

//	private Map<String, Boolean> cachedVariablesNames = new HashMap<String, Boolean>();

	private String getSelectPart(String columns) {
		return getSelectPart(columns, Boolean.FALSE);
	}

	private String getSelectPart(String columns, boolean distinct) {
		return "select ".concat(distinct ? "distinct " : CoreConstants.EMPTY).concat(columns);
	}

	@Override
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
			query = getQuery(getSelectPart(selectColumns, !full), FROM, ", JBPM_PROCESSINSTANCE pi, JBPM_PROCESSDEFINITION pd where pd.NAME_ = '",
					processDefinitionName, "' and pd.id_ = pi.PROCESSDEFINITION_ and pi.id_ = var.processinstance_ and ", CONDITION
			);
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process definition: " +
					processDefinitionName, e);
		}

		return getConverted(data, columns);
	}

	@Override
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

	@Override
	public Collection<VariableInstanceInfo> getFullVariablesByProcessDefinition(String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName, true);
	}

	@Override
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
			query = getQuery(getSelectPart(selectColumns), getFromClause(mirrow), " where var.PROCESSINSTANCE_ = ",
					String.valueOf(processInstanceId), VAR_DEFAULT_CONDITION, getMirrowTableCondition(mirrow));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by process instance: " +
					processInstanceId, e);
		}

		return getConverted(data, full ? FULL_COLUMNS : COLUMNS);
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, false, isDataMirrowed());
	}

	@Override
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, true, isDataMirrowed());
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names) {
		return getVariablesByProcessInstanceIdAndVariablesNames(procIds, true, names);
	}
	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, boolean checkCache,
			List<String> names) {
		return getVariablesByProcessesAndVariablesNames(procIds, null, names, isDataMirrowed(), false, checkCache);
	}

	@Override
	public Collection<VariableInstanceInfo> getVariableByProcessInstanceIdAndVariableName(Long procId, String name) {
		if (procId == null || StringUtil.isEmpty(name))
			return null;
		return getVariablesByProcessesAndVariablesNames(Arrays.asList(procId), null, Arrays.asList(name), isDataMirrowed(), false, false);
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names,
			boolean checkTaskInstance) {
		return getVariablesByProcessesAndVariablesNames(procIds, null, names, isDataMirrowed(), checkTaskInstance, true);
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessDefsAndVariablesNames(List<String> procDefs, List<String> names) {
		return getVariablesByProcessesAndVariablesNames(null, procDefs, names, isDataMirrowed(), true, true);
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds,
			boolean checkTaskInstance, boolean addEmptyVars) {
		return getVariablesByProcessInstanceIdAndVariablesNames(names, procIds, checkTaskInstance, addEmptyVars, true);
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds,
			boolean checkTaskInstance, boolean addEmptyVars, boolean mirrowData) {

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(names, null, ListUtil.isEmpty(procIds) ? null : new ArrayList<Long>(procIds));
		}

		if (ListUtil.isEmpty(procIds)) {
			LOGGER.warning("Process instance(s) unkown");
			return null;
		}

		return getVariablesByProcessesAndVariablesNames(names, null, procIds, checkTaskInstance, addEmptyVars, mirrowData);
	}

	private Collection<VariableInstanceInfo> getVariablesByProcessesAndVariablesNames(List<String> variablesNames, List<String> procDefNames,
			Collection<Long> procIds, boolean checkTaskInstance, boolean addEmptyVars, boolean mirrowData) {

		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("Variable name(s) unknown");
			return Collections.emptyList();
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

			Collection<VariableInstanceInfo> stringVars = getVariablesByProcessesAndVariablesNames(procIds, procDefNames, stringVariables,
					mirrowData, checkTaskInstance, true);
			Collection<VariableInstanceInfo> otherVars = getVariablesByProcessesAndVariablesNames(procIds, procDefNames, notStringVariables,
					false, checkTaskInstance, true);

			fillMapWithData(vars, variablesNames, stringVars);
			fillMapWithData(vars, variablesNames, otherVars);
		} else {
			Collection<VariableInstanceInfo> allVars = getVariablesByProcessesAndVariablesNames(procIds, procDefNames, variablesNames, mirrowData,
					checkTaskInstance, true);
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
		if (name.startsWith(VariableInstanceType.STRING.getPrefix()) || name.endsWith("officerIsMeterMaid")) {
			return new VariableStringInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
					name.startsWith(VariableInstanceType.LIST.getPrefix()) ||
					name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix())) {
			return new VariableByteArrayInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.DATE.getPrefix())) {
			return new VariableDateInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.DOUBLE.getPrefix())) {
			return new VariableDoubleInstance(name, null);
		} else if (name.startsWith(VariableInstanceType.LONG.getPrefix()) || name.equals("officerID")) {
			return new VariableLongInstance(name, null);
		} else {
			LOGGER.warning("Can not resolve variable type from name: " + name + ". Will use default one");
			return VariableInstanceInfo.getDefaultVariable(name);
		}
	}

	private void fillMapWithData(Map<Integer, VariableInstanceInfo> vars, List<String> names, Collection<VariableInstanceInfo> variables) {
		if (ListUtil.isEmpty(variables))
			return;

		List<String> addedVars = new ArrayList<String>();
		for (VariableInstanceInfo var : variables) {
			String name = var.getName();
			Long processInstance = var.getProcessInstanceId();
			int key = getKey(names.indexOf(name), processInstance);
			if (processInstance != null && addedVars.contains(name + processInstance)) {
				VariableInstanceInfo addedVar = vars.get(key);
				if (addedVar != null && addedVar.getValue() != null)
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

//	@Override
//	public List<String> getValuesByVariableFromMirrowedTable(String name) {
//		if (StringUtil.isEmpty(name))
//			return null;
//
//		List<Serializable[]> data = null;
//		String query = "select distinct m." + BPMVariableData.COLUMN_VALUE + " from " + BPMVariableData.TABLE_NAME +
//				" m, JBPM_VARIABLEINSTANCE v where v.NAME_ = '" + name + "' and v.ID_ = m." + BPMVariableData.COLUMN_VARIABLE_ID +
//				" order by m." + BPMVariableData.COLUMN_VALUE;
//		try {
//			data = SimpleQuerier.executeQuery(query, 1);
//		} catch (Exception e) {
//			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
//		}
//		if (ListUtil.isEmpty(data))
//			return null;
//
//		List<String> results = new ArrayList<String>();
//		for (Serializable[] value: data) {
//			if (ArrayUtil.isEmpty(value))
//				continue;
//
//			results.add(value[0].toString());
//		}
//		return results;
//	}

	@Override
	public List<String> getValuesByVariable(String name) {
		if (StringUtil.isEmpty(name))
			return null;

		if (isLuceneQueryingTurnedOn()) {
			List<VariableInstanceInfo> vars = getVariablesCollectionByLucene(Arrays.asList(name), null, null);
			if (ListUtil.isEmpty(vars))
				return null;

			Map<String, Boolean> values = new HashMap<String, Boolean>();
			for (VariableInstanceInfo var: vars) {
				Serializable value = var.getValue();
				if (value instanceof String)
					values.put((String) value, Boolean.TRUE);
			}
			return new ArrayList<String>(values.keySet());
		}

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

	private Collection<VariableInstanceInfo> getVariablesByProcessesAndVariablesNames(Collection<Long> procInstIds, List<String> procDefNames,
			List<String> variablesNames, boolean mirrow, boolean checkTaskInstance, boolean checkCache) {

		if (isLuceneQueryingTurnedOn() && ListUtil.isEmpty(procDefNames)) {
			return getVariablesCollectionByLucene(variablesNames, procDefNames, ListUtil.isEmpty(procInstIds) ? null : new ArrayList<Long>(procInstIds));
		}

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
		if (checkCache) {
			for (String name: variablesNames) {
				List<VariableInstanceInfo> vars = getCachedVariables(name);
				if (ListUtil.isEmpty(vars))
					continue;

				cachedVars.add(name);

				if (ListUtil.isEmpty(procInstIds)) {
					allVariables.addAll(vars);
				} else {
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
		}

		boolean anyStringColumn = false;
		for (Iterator<String> namesIter = variablesNames.iterator(); (!anyStringColumn && namesIter.hasNext());)
			anyStringColumn = namesIter.next().contains(VariableInstanceType.STRING.getPrefix());

		Collection<VariableInstanceInfo> vars = null;

		boolean byProcInstIds = !ListUtil.isEmpty(procInstIds);

		String query = null;
		int columns = anyStringColumn ? FULL_COLUMNS : FULL_COLUMNS - 1;
		try {
			String varNamesIn = getQueryParameters("var.NAME_", variablesNames, true);
			query = getQuery(getSelectPart(anyStringColumn ? getFullColumns(mirrow, false) : getAllButStringColumn(), false),
					mirrow ?
							" from ".concat(BPMVariableData.TABLE_NAME).concat(" mirrow, ").concat(VARIABLES_TABLE).concat(" var ") :
							getFromClause(true, mirrow),
					checkTaskInstance ?
							", jbpm_taskinstance task " :
							CoreConstants.EMPTY,
					byProcInstIds ?
							CoreConstants.EMPTY :
							", JBPM_PROCESSINSTANCE pi, JBPM_PROCESSDEFINITION pd ",
					" where ",
					byProcInstIds ?
							PROC_INST_IDS_EXPRESSION :
							getQueryParameters("pd.name_", procDefNames, true) + " and pd.ID_ = pi.PROCESSDEFINITION_ and pi.ID_ = " +
								"var.PROCESSINSTANCE_ ",
					mirrow ?
							getMirrowTableCondition(true) :
							CoreConstants.EMPTY,
					checkTaskInstance ?
							" and var.TASKINSTANCE_ = task.ID_ and task.END_ is not null " :
							CoreConstants.EMPTY,
					" and " + varNamesIn, " and ", CLASS_CONDITION + " order by var.TASKINSTANCE_");
			vars = byProcInstIds ?
					getVariablesByProcessInstanceIds(query, columns, new ArrayList<Long>(procInstIds)) :
					getVariablesByQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables for process instance(s) : " + procInstIds +
					" and name(s): "
					+ variablesNames, e);
		}

		if (ListUtil.isEmpty(allVariables))
			return vars;

		if (!ListUtil.isEmpty(vars))
			allVariables.addAll(vars);

		return allVariables;
	}

	@Override
	public boolean isVariableCached(String name) {
		if (StringUtil.isEmpty(name))
			return false;

		return getVariablesCache().get(name) != null;
	}

	@Override
	public boolean isVariableStored(String name, Serializable value) {
		if (isVariableCached(name) && getCachedVariable(name, value) != null)
			return true;

		Collection<VariableInstanceInfo> variables = getProcessVariablesByNameAndValue(name, value, false);
		if (ListUtil.isEmpty(variables))
			return false;

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

	@Override
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(String name, Serializable value) {
		return getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(name, value, null);
	}

	@Override
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(String name, Serializable value, List<Long> procInstIds) {
		VariableInstanceInfo cachedVariable = getCachedVariable(name, value);
		boolean useCaching = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getBoolean("use_cached_proc_inst_ids",
				true);

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

	@Override
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

	/**
	 * <p>Constructs part of query.</p>
	 * @param columnName name of column in database for comparison.
	 * @param value {@link BPMProcessVariable#getRealValue()}
	 * @param searchSymbol {@link VariableQuerierData#getSearchExpression()}.
	 * @return {@link String} part of query, which should be used in database.
	 * <code>null</code> if unsuccessful.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	private String getColumnAndValueExpression(String columnName, Serializable value, String searchSymbol, boolean flexible) {
		if (value == null)
			return null;

		StringBuilder expression = new StringBuilder();
		boolean searchExpression = flexible && !StringUtil.isEmpty(searchSymbol);

		if (!searchExpression)
			searchSymbol = CoreConstants.EQ;

		if (value instanceof String) {
			expression.append(searchExpression ? " lower(" : CoreConstants.EMPTY)
			.append(columnName)
			.append(searchExpression ? ")" : CoreConstants.EMPTY)
			.append(" like ")
			.append(searchExpression ? "lower(" : CoreConstants.EMPTY)
			.append(CoreConstants.QOUTE_SINGLE_MARK)
			.append(searchExpression ? CoreConstants.PERCENT : CoreConstants.EMPTY)
			.append(getStringValueExpression(value.toString().trim()))
			.append(searchExpression ? CoreConstants.PERCENT : CoreConstants.EMPTY)
			.append(CoreConstants.QOUTE_SINGLE_MARK)
			.append(searchExpression ? ")" : CoreConstants.EMPTY);
		} else if (value instanceof Number) {
			expression.append(columnName)
			.append(CoreConstants.SPACE)
			.append(searchSymbol)
			.append(CoreConstants.SPACE)
			.append(value);
		} else if (value instanceof Timestamp) {
			expression.append(columnName)
			.append(CoreConstants.SPACE)
			.append(searchSymbol)
			.append(CoreConstants.SPACE)
			.append(CoreConstants.QOUTE_SINGLE_MARK)
			.append(value.toString())
			.append(CoreConstants.QOUTE_SINGLE_MARK);
		} else {
			LOGGER.warning("Do not know how to build column and value expression for column '" + columnName + "' and value '" +
					value+ "' and search symbol: " + searchSymbol);
			return null;
		}

		return expression.toString();
	}

	BPMContext getBpmContext() {
		if (bpmContext == null)
			ELUtil.getInstance().autowire(this);
		return bpmContext;
	}

	private Collection<VariableInstanceInfo> getVariablesByNameAndValuesAndProcessDefinitions(String name, List<String> procDefNames,
			Collection<?> values, List<Long> procInstIds) {

		Map<Long, BinaryVariableData> data = getDataByVariablesByNameAndValuesAndProcessDefinitions(name, procDefNames, values, procInstIds);
		if (MapUtil.isEmpty(data))
			return null;

		List<VariableInstanceInfo> vars = new ArrayList<VariableInstanceInfo>();
		for (BinaryVariableData binVarData: data.values())
			vars.add(binVarData.getVariable());
		return vars;
	}

	class BinaryVariableData {
		private Long procInstId;
		private Serializable[] rawData;
		private VariableInstanceInfo variable;

		BinaryVariableData(Long procInstId, Serializable[] rawData, VariableInstanceInfo variable) {
			this.procInstId = procInstId;
			this.rawData = rawData;
			this.variable = variable;
		}

		Long getProcessInstanceId() {
			return procInstId;
		}

		Serializable[] getRawData() {
			return rawData;
		}

		VariableInstanceInfo getVariable() {
			return variable;
		}

		@Override
		public String toString() {
			return getProcessInstanceId() + ": " + getVariable() + ". Raw data: " + getRawData();
		}
	}

	private Map<Long, BinaryVariableData> getDataByVariablesByNameAndValuesAndProcessDefinitions(String name, List<String> procDefNames,
			Collection<?> values, List<Long> procInstIds) {

		if (StringUtil.isEmpty(name))
			return Collections.emptyMap();

		int columns = COLUMNS + 2;
		boolean byProcInst = !ListUtil.isEmpty(procInstIds);
		//	Getting all binary variables
		String query = getQuery(getSelectPart(STANDARD_COLUMNS), ", b.BYTES_, var.PROCESSINSTANCE_ ", getFromClause(true, false),
				", JBPM_BYTEBLOCK b",
					byProcInst ?
						CoreConstants.EMPTY :
						", JBPM_PROCESSINSTANCE pi, JBPM_PROCESSDEFINITION pd ",
				" where b.PROCESSFILE_ = var.BYTEARRAYVALUE_ ",
					byProcInst ?
						" and " + PROC_INST_IDS_EXPRESSION :
						getQueryParameters(" and pd.name_", procDefNames, true).concat(" and var.name_ = '").concat(name).concat("'") +
						" and pd.ID_ = pi.PROCESSDEFINITION_ and pi.ID_ = var.PROCESSINSTANCE_",
				" and var.CLASS_ = 'B' order by var.TASKINSTANCE_"
		);
		List<Serializable[]> data = byProcInst ?
				getDataByProcessInstanceIds(null, query, columns, procInstIds) :
				getDataByQuery(query, columns);
		if (ListUtil.isEmpty(data))
			return Collections.emptyMap();

		Collection<VariableInstanceInfo> resolvedVars = null;
		Collection<VariableInstanceInfo> vars = getConverted(data, columns);
		MultipleSelectionVariablesResolver resolver = getMultipleValuesResolver(name);
		if (resolver == null) {
			if (ListUtil.isEmpty(vars))
				return Collections.emptyMap();

			List<String> addedVars = new ArrayList<String>();
			Collection<VariableInstanceInfo> uniqueVars = new ArrayList<VariableInstanceInfo>();
			for (VariableInstanceInfo var: vars) {
				String key = getKeyForVariable(var, false);
				if (addedVars.contains(key))
					continue;

				Object value = var.getValue();
				if (value instanceof Collection<?>) {
					Collection<?> realValue = (Collection<?>) value;
					if (!CollectionUtils.isEqualCollection(realValue, values))
						continue;

					uniqueVars.add(var);
					addedVars.add(key);
				}
			}

			resolvedVars = uniqueVars;
		} else
			resolvedVars = getResolvedVariables(resolver, vars, values);

		if (ListUtil.isEmpty(resolvedVars))
			return Collections.emptyMap();

		Map<Long, VariableInstanceInfo> resolvedProcInstIds = new HashMap<Long, VariableInstanceInfo>();
		Map<Long, BinaryVariableData> results = new HashMap<Long, VariableInstanceQuerierImpl.BinaryVariableData>();
		for (Serializable[] rawData: data) {
			if (ArrayUtil.isEmpty(rawData))
				continue;

			Long procInstId = null;
			Serializable id = rawData[rawData.length - 1];
			if (id instanceof Number)
				procInstId = ((Number) id).longValue();
			if (procInstId == null)
				continue;

			if (resolvedProcInstIds.size() == 0)
				for (VariableInstanceInfo var: resolvedVars)
					resolvedProcInstIds.put(var.getProcessInstanceId(), var);

			if (!resolvedProcInstIds.containsKey(procInstId))
				continue;

			results.put(procInstId, new BinaryVariableData(procInstId, rawData, resolvedProcInstIds.get(procInstId)));
		}
		return results;
	}

	private MultipleSelectionVariablesResolver getMultipleValuesResolver(String varName) {
		MultipleSelectionVariablesResolver resolver = null;
		try {
			resolver = ELUtil.getInstance().getBean(MultipleSelectionVariablesResolver.BEAN_NAME_PREFIX + varName);
		} catch (Exception e) {}
		return resolver;
	}

	private Collection<VariableInstanceInfo> getResolvedVariables(MultipleSelectionVariablesResolver resolver, Collection<VariableInstanceInfo> vars,
			Collection<?> values) {

		Collection<AdvancedProperty> resolvedValues = resolver == null ? null : resolver.getBinaryVariablesValues(vars, values);
		if (ListUtil.isEmpty(resolvedValues))
			return null;

		List<String> addedVars = new ArrayList<String>();
		Collection<VariableInstanceInfo> resolvedVars = new ArrayList<VariableInstanceInfo>();

		//	Checking if resolver provided final results
		Collection<VariableInstanceInfo> finalResult = resolver == null ? null : resolver.getFinalSearchResult();
		if (!ListUtil.isEmpty(finalResult)) {
			for (VariableInstanceInfo var: finalResult) {
				String key = getKeyForVariable(var, true).concat(String.valueOf(var.getProcessInstanceId()));
				if (!addedVars.contains(key)) {
					resolvedVars.add(var);
					addedVars.add(key);
				}
			}
			return resolvedVars;
		}

		//	Final results were not provided - trying to find the variables satisfying user query
		List<String> resolvedIds = new ArrayList<String>();
		for (AdvancedProperty resolved: resolvedValues) {
			for (Object value: values) {
				if (value instanceof Serializable && resolved.getId().equals(value.toString()) && !resolvedIds.contains(value.toString())) {
					resolvedIds.add(value.toString());
				}
			}
		}
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

	@Override
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames) {
		return getProcessVariablesByNameAndValue(name, values, procDefNames, null, true, true, isDataMirrowed());
	}

	@Override
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(
			String name,
			List<Serializable> values,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean searchExpression,
			boolean mirrow) {

		VariableQuerierData dataList = new VariableQuerierData(name, values);

		return getProcessVariablesByNameAndValue(name, dataList, procDefNames, procInstIds, selectProcessInstanceId, mirrow);
	}

	@Override
	public Collection<VariableInstanceInfo> getProcessVariablesByNameAndValue(
			String name,
			VariableQuerierData data,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean mirrow) {

		if (StringUtil.isEmpty(name) || data == null)
			return Collections.emptyList();

		List<Serializable> values = data.getValues();
		if (values instanceof Collection<?>	&& (
				name.startsWith(VariableInstanceType.LIST.getPrefix()) ||
				name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
				name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix())))

			//	Binary data, need specific handling
			return getVariablesByNameAndValuesAndProcessDefinitions(name, procDefNames, values, procInstIds);

		return getProcessVariablesByNamesAndValues(Arrays.asList(data), null, procDefNames, procInstIds, selectProcessInstanceId, mirrow, false);
	}

	private Collection<VariableInstanceInfo> getProcessVariablesByNamesAndValues(
			List<VariableQuerierData> data,
			List<String> variablesWithoutValues,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean mirrow,
			boolean strictBinaryVariables) {

		if (isLuceneQueryingTurnedOn()) {
			Map<String, VariableQuerierData> activeVariables = null;
			if (!ListUtil.isEmpty(data)) {
				activeVariables = new HashMap<String, VariableQuerierData>();
				for (VariableQuerierData dataItem: data) {
					activeVariables.put(dataItem.getName(), dataItem);
				}
			}
			return getVariablesCollectionByLucene(
					activeVariables,
					variablesWithoutValues,
					procDefNames,
					procInstIds,
					null,
					null,
					null,
					strictBinaryVariables
			);
		}

		List<Serializable[]> queriedData = getInformationByVariablesNameAndValuesAndProcesses(data, variablesWithoutValues, procDefNames,
				procInstIds, selectProcessInstanceId, mirrow, strictBinaryVariables);
		if (ListUtil.isEmpty(queriedData))
			return Collections.emptyList();

		//	Converting raw data into the objects
		int numberOfColumns = queriedData.iterator().next().length;
		return getConverted(queriedData, numberOfColumns);
	}

	private List<Serializable[]> getInformationByVariablesNameAndValuesAndProcesses(
			List<VariableQuerierData> data,
			List<String> variablesWithoutValues,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean mirrow,
			boolean strictBinaryVariables) {

		return getInformationByVariablesNameAndValuesAndProcesses(
				null, null, null,
				data,
				variablesWithoutValues,
				procDefNames,
				procInstIds,
				null, null,
				selectProcessInstanceId,
				mirrow,
				strictBinaryVariables
		);
	}

	private List<Serializable[]> getInformationByVariablesNameAndValuesAndProcesses(
			String columnsToSelect,
			String groupBy,
			Integer numberOfColumns,

			List<VariableQuerierData> data,
			List<String> variablesWithoutValues,

			List<String> procDefNames,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			boolean selectProcessInstanceId,
			boolean mirrow,
			boolean strictBinaryVariables
	) {

		boolean anyStringColumn = false;
		boolean useBinding = true;
		String columnPrefix = "var";
		String mirrowPrefix = MIRRROW_TABLE_ALIAS;
		String columnName = columnPrefix.concat(CoreConstants.DOT);
		String currentColumnPrefix = null;
		String allValuesClause = CoreConstants.EMPTY;
		StringBuffer valuesToSelect = new StringBuffer();
		Map<Long, List<Serializable[]>> binaryResults = null;
		Map<String, String> mirrows = new HashMap<String, String>();
		Map<String, String> fromParts = new HashMap<String, String>();
		Map<String, VariableInstanceType> types = new HashMap<String, VariableInstanceType>();

		if (!ListUtil.isEmpty(data)) {
			for (Iterator<VariableQuerierData> queryPartIter = data.iterator(); queryPartIter.hasNext();) {
				VariableQuerierData queryPart = queryPartIter.next();

				String variableName = queryPart.getName();
				List<Serializable> values = queryPart.getValues();
				if (ListUtil.isEmpty(values)) {
					LOGGER.warning("Values for variable '" + variableName + "' are not provided, skipping querying");
					return Collections.emptyList();
				}

				Serializable value = values.iterator().next();
				if (value == null) {
					LOGGER.warning("Value for variable '" + variableName + "' must be specified!");
					return Collections.emptyList();
				}

				if (currentColumnPrefix == null) {
					if (data.size() == 1 && ListUtil.isEmpty(variablesWithoutValues)) {
						useBinding = false;
						currentColumnPrefix = columnPrefix;
					} else
						currentColumnPrefix = "v".concat(currentColumnPrefix == null ? columnPrefix : currentColumnPrefix);
				} else {
					currentColumnPrefix = "v".concat(currentColumnPrefix);
				}

				String currentColumnName = currentColumnPrefix.concat(CoreConstants.DOT);

				if (values instanceof Collection<?>	&& (
						variableName.startsWith(VariableInstanceType.LIST.getPrefix()) ||
						variableName.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
						variableName.startsWith(VariableInstanceType.OBJ_LIST.getPrefix()))) {

					//	Binary data, need specific handling
					Map<Long, BinaryVariableData> binaryData = getDataByVariablesByNameAndValuesAndProcessDefinitions(variableName, procDefNames,
							queryPart.getValues(), procInstIds);
					if (MapUtil.isEmpty(binaryData)) {
						LOGGER.warning("No variables were found by name " + variableName + ", values " + queryPart.getValues() +
								", proc. def. names " + procDefNames + " and proc. inst. IDs " + procInstIds);
						return Collections.emptyList();
					}

					if (binaryResults == null)
						binaryResults = new HashMap<Long, List<Serializable[]>>();

					for (BinaryVariableData binVarData: binaryData.values()) {
						List<Serializable[]> rawData = binaryResults.get(binVarData.getProcessInstanceId());
						if (rawData == null) {
							rawData = new ArrayList<Serializable[]>();
							binaryResults.put(binVarData.getProcessInstanceId(), rawData);
						}
						rawData.add(binVarData.getRawData());
					}

					continue;

				//	Determining variable type
				} else if (value instanceof String) {
					anyStringColumn = true;
					currentColumnName = getStringValueColumn(currentColumnPrefix, mirrowPrefix, mirrow);
					currentColumnName = mirrow ? currentColumnName : getSubstring(currentColumnName);
					types.put(currentColumnPrefix, VariableInstanceType.STRING);
					mirrows.put(currentColumnPrefix, mirrowPrefix);
					mirrowPrefix = "m".concat(mirrowPrefix);
				} else if (value instanceof Long) {
					currentColumnName = currentColumnName.concat("LONGVALUE_");
					types.put(currentColumnPrefix, VariableInstanceType.LONG);
				} else if (value instanceof Double) {
					currentColumnName = currentColumnName.concat("DOUBLEVALUE_");
					types.put(currentColumnPrefix, VariableInstanceType.DOUBLE);
				} else if (value instanceof Timestamp) {
					currentColumnName = currentColumnName.concat("DATEVALUE_");
					types.put(currentColumnPrefix, VariableInstanceType.DATE);
				} else {
					LOGGER.warning("Unsupported type of value: " + value + ", "	+ value.getClass());
					return Collections.emptyList();
				}

				if (values.size() > 1) {
					//	Multiple values
					if (currentColumnName.indexOf("string") == -1) {
						//	Not string values
						valuesToSelect.append(currentColumnName).append(" in (");
						for (Iterator<Serializable> valuesIter = values.iterator();	valuesIter.hasNext();) {
							valuesToSelect.append(valuesIter.next());

							if (valuesIter.hasNext())
								valuesToSelect.append(", ");
						}
						valuesToSelect.append(") ");
					} else {
						//	String values
						valuesToSelect.append("(");
						for (Iterator<Serializable> valuesIter = values.iterator(); valuesIter.hasNext();) {
							valuesToSelect.append(getColumnAndValueExpression(currentColumnName, valuesIter.next(), queryPart.getSearchExpression(),
									queryPart.isFlexible()));

							if (valuesIter.hasNext())
								valuesToSelect.append(" or ");
						}
						valuesToSelect.append(")");
					}
				} else {
					//	Single value
					valuesToSelect.append(getColumnAndValueExpression(currentColumnName, values.get(0), queryPart.getSearchExpression(),
							queryPart.isFlexible()));
				}

				valuesToSelect.append(" and ").append(currentColumnPrefix).append(".name_ = '").append(variableName).append("' ");
				if (variablesWithoutValues == null)
					variablesWithoutValues = new ArrayList<String>();
				else
					variablesWithoutValues = new ArrayList<String>(variablesWithoutValues);
				if (!variablesWithoutValues.contains(variableName))
					variablesWithoutValues.add(variableName);

				fromParts.put(currentColumnPrefix, currentColumnName);

				if (queryPartIter.hasNext())
					valuesToSelect.append(" and ");
				}
//			}
		}
		allValuesClause = valuesToSelect.toString();
		if (!StringUtil.isEmpty(allValuesClause))
			allValuesClause = " and ".concat(allValuesClause);

		List<Long> tmpIds = null;
		if (binaryResults == null || !strictBinaryVariables)
			tmpIds = procInstIds;
		else
			tmpIds = new ArrayList<Long>(binaryResults.keySet());
		boolean byProcessInstances = !ListUtil.isEmpty(tmpIds);
		boolean byProcDefs = !ListUtil.isEmpty(procDefNames);

		String query = null;
		int columns = COLUMNS + 5;
		columns = numberOfColumns == null ? (columns + (selectProcessInstanceId ? 1 : 0)) : numberOfColumns;
		String select = null;
		if (StringUtil.isEmpty(columnsToSelect)) {
			select = getSelectPart(STANDARD_COLUMNS + ", " + getSubstring(getStringValueColumn(false)) +
					", var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.BYTEARRAYVALUE_ as bv ", false);
			select = select.concat(selectProcessInstanceId || byProcessInstances ? ", var.PROCESSINSTANCE_ as piid" : CoreConstants.EMPTY);
		} else
			select = columnsToSelect;

		List<String> parts = new ArrayList<String>(Arrays.asList(select, " from JBPM_VARIABLEINSTANCE var ",
			byProcessInstances ?
					CoreConstants.EMPTY
					: byProcDefs ?
							", JBPM_PROCESSINSTANCE pi, JBPM_PROCESSDEFINITION pd "
							: CoreConstants.EMPTY
		));

		if (mirrow && mirrows.size() > 0) {
			parts.add(CoreConstants.COMMA);
			for (Iterator<String> keysIter = mirrows.keySet().iterator(); keysIter.hasNext();) {
				parts.add(CoreConstants.SPACE.concat(BPMVariableData.TABLE_NAME.concat(CoreConstants.SPACE)
						.concat(mirrows.get(keysIter.next()))));

				if (keysIter.hasNext())
					parts.add(CoreConstants.COMMA.concat(CoreConstants.SPACE));
			}
		}

		StringBuffer bindingExpression = useBinding ? new StringBuffer() : null;
		if (useBinding) {
			for (Map.Entry<String, String> fromPart: fromParts.entrySet()) {
				parts.add(", JBPM_VARIABLEINSTANCE ".concat(fromPart.getKey()));

				bindingExpression.append(" and ").append(columnName).append("processinstance_ = ").append(fromPart.getKey())
					.append(".processinstance_");
			}
		}

		parts.addAll(Arrays.asList(" where ",
			byProcessInstances ?
					PROC_INST_IDS_EXPRESSION
					: byProcDefs ?
							getQueryParameters("pd.name_", procDefNames, true) + " and pd.ID_ = pi.PROCESSDEFINITION_ and pi.id_ = " +
								" var.PROCESSINSTANCE_ "
							: CoreConstants.EMPTY
		));

		if (!ListUtil.isEmpty(taskInstIds)) {
			parts.add(getQueryParameters("var.TASKINSTANCE_", taskInstIds, false));
		}
		if (!ListUtil.isEmpty(varIds)) {
			parts.add(getQueryParameters("var.id_", varIds, false));
		}

		if (useBinding && !ListUtil.isEmpty(variablesWithoutValues)) {
			if (byProcessInstances || byProcDefs)
				parts.add(" and ");
			parts.add(getQueryParameters(columnName.concat("name_"), variablesWithoutValues, true));
		}

		if (bindingExpression != null)
			parts.add(bindingExpression.toString());

		if (!MapUtil.isEmpty(types)) {
			if (byProcDefs || byProcessInstances)
				parts.add(" and ");
			for (Iterator<Map.Entry<String, VariableInstanceType>> typesIter = types.entrySet().iterator(); typesIter.hasNext();) {
				Map.Entry<String, VariableInstanceType> type = typesIter.next();
				parts.add(getQueryParameters(type.getKey().concat(".class_"), type.getValue().getTypeKeys(), true));
				if (typesIter.hasNext())
					parts.add(" and ");
			}
		}
		parts.add(" and var.class_ <> 'N' ");

		parts.add(allValuesClause);
		if (anyStringColumn && mirrow) {
			for (Map.Entry<String, String> mirrowEntry: mirrows.entrySet()) {
				parts.add(getMirrowTableCondition(mirrowEntry.getKey().concat(".id_"), mirrowEntry.getValue()));
			}
		}
		if (!StringUtil.isEmpty(groupBy))
			parts.add(" ".concat(groupBy));
		parts.add(" order by var.TASKINSTANCE_");
		query = getQuery(ArrayUtil.convertListToArray(parts));

		List<Serializable[]> results = byProcessInstances ?
				getDataByProcessInstanceIds(null, query, columns, tmpIds) :
				getDataByQuery(query, columns);

		if (ListUtil.isEmpty(results) && binaryResults != null) {
			//	Returning just binary variables if any were found
			results = new ArrayList<Serializable[]>();
			for (List<Serializable[]> rawData: binaryResults.values())
				results.addAll(rawData);
			return results;
		}

		if (!MapUtil.isEmpty(binaryResults)) {
			//	Combining binary variables with other variables
			for (List<Serializable[]> rawData: binaryResults.values())
				results.addAll(rawData);
		}

		return results;
	}

	/**
	 * <p>Appends vars with new variables founded in database.</p>
	 * @param vars {@link Collection} of {@link VariableInstanceInfo} or
	 * <code>null</code>.
	 * @param query to database.
	 * @param columns number in response of database.
	 * @param procInstIds {@link List} of {@link ProcessInstance#getId()}.
	 * @return {@link Collection} of {@link VariableInstanceInfo} or
	 * {@link Collections#emptyList()}.
	 */
	private Collection<VariableInstanceInfo> getVariablesByProcessInstanceIds(
			String query,
			int columns,
			List<Long> procInstIds) {
		List<Serializable[]> data = getDataByProcessInstanceIds(null, query, columns, procInstIds);
		return getConverted(data, columns);
	}

	private List<Serializable[]> getDataByProcessInstanceIds(List<Serializable[]> data, String query, int columns, List<Long> procInstIds) {
		if (data == null)
			data = new ArrayList<Serializable[]>();
		if (ListUtil.isEmpty(procInstIds))
			return data;

		List<Long> usedIds = null;
		if (procInstIds.size() > 1000) {
			usedIds = new ArrayList<Long>(procInstIds.subList(0, 1000));
			procInstIds = new ArrayList<Long>(procInstIds.subList(1000,	procInstIds.size()));
		} else {
			usedIds = new ArrayList<Long>(procInstIds);
			procInstIds = null;
		}

		List<Serializable[]> queriedData = getDataByQuery(
				StringHandler.replace(query, PROC_INST_IDS_EXPRESSION, getQueryParameters("var.PROCESSINSTANCE_", usedIds, false)),
				columns
		);

		if (queriedData != null)
			data.addAll(queriedData);

		return getDataByProcessInstanceIds(data, query, columns, procInstIds);
	}

	/**
	 * <p>Queries database. Converts database data to {@link List} of
	 * {@link VariableInstanceInfo}.</p>
	 * @param query to database.
	 * @param columns number selected from database in response.
	 * @return {@link Collection} of {@link VariableInstanceInfo} or
	 * <code>null</code> on failure;
	 */
	private Collection<VariableInstanceInfo> getVariablesByQuery(String query, int columns) {
		return getConverted(getDataByQuery(query, columns), columns);
	}

	private List<Serializable[]> getDataByQuery(String query, int columns) {
		try {
			return SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'", e);
		}
		return null;
	}

	/**
	 *
	 * @param namesAndValues {@link Map} of ({@link BPMProcessVariable#getName()}
	 * {@link BPMProcessVariable#getValue()}).
	 * @param variables {@link List} of {@link BPMProcessVariable#getName()}.
	 * @param procDefNames {@link ProcessDefinition#getName()}.
	 * @param procInstIds {@link ProcessInstance#getId()}.
	 * @param selectProcessInstanceId
	 * @param cachedVariables
	 * @param flexibleVariables {@link Map} of (
	 * {@link BPMProcessVariable#getName()},
	 * {@link BPMProcessVariable#isFlexible()}).
	 * @return {@link Collection} of {@link VariableInstanceInfo} or
	 * <code>null</code> on failure.
	 */
	private Collection<VariableInstanceInfo> getProcessVariablesByNamesAndValues(
			Map<String, VariableQuerierData> namesAndValues,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables) {

		if (namesAndValues == null) {
			LOGGER.warning("Variables names and values are not provided");
			return null;
		}

		List<VariableQuerierData> data = new ArrayList<VariableQuerierData>(namesAndValues.values());

		if (!MapUtil.isEmpty(flexibleVariables)) {
			for (VariableQuerierData queryItem: data) {
				Boolean flexible = flexibleVariables.get(queryItem.getName());
				if (flexible == null)
					flexible = Boolean.FALSE;

				if (flexible)
					queryItem.setSearchExpression(" like ");

				queryItem.setFlexible(flexible);
			}
		}

		Collection<VariableInstanceInfo> results = getProcessVariablesByNamesAndValues(data, variables, procDefNames, procInstIds,
				selectProcessInstanceId, isDataMirrowed(), strictBinaryVariables);

		return results;
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

	@Override
	public Collection<VariableInstanceInfo> getConverted(List<Serializable[]> data, int numberOfColumns) {
		if (ListUtil.isEmpty(data))
			return null;

		boolean sort = true;

		Map<Number, VariableInstanceInfo> variables = new HashMap<Number, VariableInstanceInfo>();
		Map<Number, VariableByteArrayInstance> variablesToConvert = new HashMap<Number, VariableByteArrayInstance>();
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
				LOGGER.warning("Unable to create variable from initial values (" + dataSet + "): ID: " + id	+ ", name: " + name	+ ", class: " + type);
				continue;
			}

			Serializable value = null;
			Long piId = null;
			String caseId = null;
			if (numberOfColumns > COLUMNS) {
				int index = COLUMNS;
				//	Resolving variable value
				while (value == null && (index + 1) < numberOfColumns) {
					value = dataSet[index];
					index++;
				}

				//	Resolving process instance ID
				int piIdIndex = 0;
				if (numberOfColumns == FULL_COLUMNS)
					piIdIndex = numberOfColumns - 1;
				else if (numberOfColumns == FULL_COLUMNS + 1)
					piIdIndex = numberOfColumns - 2;
				else
					piIdIndex = dataSet.length - 1;
				if (piIdIndex >= index && piIdIndex < dataSet.length) {
					Serializable temp = dataSet[piIdIndex];
					if (temp instanceof Number) {
						piId = Long.valueOf(((Number) temp).longValue());
					}
				}

				//	Resolving case
				if (numberOfColumns > FULL_COLUMNS) {
					Serializable tmp = dataSet[dataSet.length - 1];
					if (tmp instanceof Number)
						caseId = String.valueOf(((Number) tmp).longValue());
				}
			}

			VariableInstanceInfo variable = variables.get(id);
			if (variable == null) {
				variable = getVariable(id, name, value, type, variablesToConvert);

				if (variable == null) {
					LOGGER.warning("Unkown variable with id: " + id + ", name: '" + name + "', type: '" + type + "' and value: " + value);
				} else {
					variable.setId(id.longValue());
					variable.setProcessInstanceId(piId);
					if (caseId != null)
						variable.setCaseId(caseId);
					variables.put(id, variable);
				}
			} else if (value != null) {
				variable.setValue(value);
			}
		}

		convertByteValues(variablesToConvert);
		for (VariableByteArrayInstance byteVar: variablesToConvert.values())
			variables.put(byteVar.getId(), byteVar);

		if (sort) {
			List<VariableInstanceInfo> vars = new ArrayList<VariableInstanceInfo>(variables.values());
			Collections.sort(vars, new VariableInstanceInfoComparator());
			return vars;
		}

		return variables.values();
	}

	private VariableInstanceInfo getVariable(Number id, String name, Serializable value, String type,
			Map<Number, VariableByteArrayInstance> variablesToConvert) {

		VariableInstanceInfo variable = null;
		if (value == null) {
			variable = new VariableDefaultInstance(name, type);
		} else if (value instanceof String || VariableInstanceType.STRING.getTypeKeys().contains(type)) {
			variable = id instanceof Number ? new VariableStringInstance(id.longValue(), name, value) :
				new VariableStringInstance(name, value);
		} else if ((value instanceof Long || value instanceof Number) && VariableInstanceType.LONG.getTypeKeys().contains(type)) {
			variable = new VariableLongInstance(name, ((Number) value).longValue());
		} else if ((value instanceof Double || value instanceof Number) && VariableInstanceType.DOUBLE.getTypeKeys().contains(type)) {
			variable = new VariableDoubleInstance(name, ((Number) value).doubleValue());
		} else if (value instanceof Timestamp && VariableInstanceType.DATE.getTypeKeys().contains(type)) {
			variable = new VariableDateInstance(name, (Timestamp) value);
		} else if (value instanceof Date && VariableInstanceType.DATE.getTypeKeys().contains(type)) {
			variable = new VariableDateInstance(name, new Timestamp(((Date) value).getTime()));
		} else if (value instanceof Byte[] || VariableInstanceType.BYTE_ARRAY.getTypeKeys().contains(type) ||
				VariableInstanceType.OBJ_LIST.getTypeKeys().contains(type)) {
			VariableByteArrayInstance byteVariable = new VariableByteArrayInstance(name, value);
			variable = byteVariable;

			if (value instanceof Number)
				variablesToConvert.put((Number) value, byteVariable);
		} else {
			// Try to execute custom methods
			java.sql.Date date = getValueFromCustomMethod(value, "dateValue");
			if (date != null) {
				variable = new VariableDateInstance(name, new Timestamp(date.getTime()));
			}
		}

		return variable;
	}

	private void convertByteValues(Map<Number, VariableByteArrayInstance> variables) {
		if (MapUtil.isEmpty(variables))
			return;

		List<Number> ids = new ArrayList<Number>(variables.keySet());
		String query = "select b.PROCESSFILE_, b.BYTES_ from JBPM_BYTEBLOCK b where ";
		List<Serializable[]> data = getBytesByIds(null, query, 2, ids);
		if (ListUtil.isEmpty(data))
			return;

		Map<Number, List<byte[]>> bytesForVariables = new HashMap<Number, List<byte[]>>();
		for (Serializable[] info: data) {
			if (ArrayUtil.isEmpty(info) || info.length == 1)
				continue;

			Number id = null;
			Serializable item = info[0];
			if (item instanceof Number)
				id = (Number) item;
			else
				continue;

			List<byte[]> existingBytes = bytesForVariables.get(id);
			if (existingBytes == null) {
				existingBytes = new ArrayList<byte[]>();
				bytesForVariables.put(id, existingBytes);
			}

			byte[] bytes = new byte[(info.length - 1) * 1024];
			int pos = 0;
			for (int i = 1; i < info.length; i++) {
				byte[] tmp = (byte[]) info[i];
				System.arraycopy(tmp, 0, bytes, pos, tmp.length);
				pos += tmp.length;
			}
			existingBytes.add(bytes);
		}

		for (Number id: bytesForVariables.keySet()) {
			VariableByteArrayInstance var = variables.get(id);
			if (var == null)
				continue;

			List<byte[]> values = bytesForVariables.get(id);
			if (ListUtil.isEmpty(values))
				continue;

			byte[] bytes = null;
			if (values.size() > 1) {
				int pos = 0;
				bytes = new byte[values.size() * 1024];
				for (Iterator<byte[]> it = values.iterator(); it.hasNext();) {
					byte[] tmp = it.next();
					System.arraycopy(tmp, 0, bytes, pos, tmp.length);
					pos += tmp.length;
				}
			} else
				bytes = values.get(0);

			var.setValue(bytes);
		}
	}

	private List<Serializable[]> getBytesByIds(List<Serializable[]> data, String query, int columns, List<Number> ids) {
		if (data == null)
			data = new ArrayList<Serializable[]>();
		if (ListUtil.isEmpty(ids))
			return data;

		List<Number> usedIds = null;
		if (ids.size() > 1000) {
			usedIds = new ArrayList<Number>(ids.subList(0, 1000));
			ids = new ArrayList<Number>(ids.subList(1000, ids.size()));
		} else {
			usedIds = new ArrayList<Number>(ids);
			ids = null;
		}

		List<Serializable[]> queriedData = getDataByQuery(query + getQueryParameters("b.PROCESSFILE_", usedIds, false), 2);

		if (queriedData != null)
			data.addAll(queriedData);

		return getBytesByIds(data, query, columns, ids);
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
		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("substring_jbpm_vars", Boolean.FALSE))
			return "substr(".concat(column).concat(", 1, 255)");
		return column;
	}

	/**
	 * <p>Creates part of query with given values added as arguments for given
	 * columnName.</p>
	 * @param columnName of database table.
	 * @param values {@link List} of {@link Serializable} to compare with.
	 * @param isString tells if passed values should be interpreted as
	 * {@link List} of {@link String}.
	 * @return part of query for database.
	 */
	private String getQueryParameters(
			String columnName,
			Collection<? extends Serializable> values,
			boolean isString) {

		return getQueryParameters(columnName, values, Boolean.FALSE, isString);
	}

	/**
	 * <p>Creates part of query with given values added as arguments for given
	 * columnName.</p>
	 * @param columnName of database table.
	 * @param values {@link List} of {@link Serializable} to compare with.
	 * @param notEquals ? " <> " : " = ".
	 * @param isString tells if passed values should be interpreted as
	 * {@link List} of {@link String}.
	 * @return part of query for database.
	 */
	private String getQueryParameters(
			String columnName,
			Collection<? extends Serializable> values,
			boolean notEquals,
			boolean isString) {

		synchronized (values) {
			StringBuilder params = new StringBuilder();
			if (values.size() == 1) {
				params.append(StringUtil.isEmpty(columnName) ?
						CoreConstants.SPACE
						: CoreConstants.SPACE.concat(columnName));

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
				params.append(StringUtil.isEmpty(columnName) ?
						" ("
						: CoreConstants.SPACE
								.concat(columnName)
								.concat(notEquals ?
										" not "
										: CoreConstants.EMPTY
								)
								.concat(" in (")
				);

				for (Iterator<? extends Serializable> iter = values.iterator();
						iter.hasNext();) {

					Serializable value = iter.next();

					if (isString) {
						params.append(CoreConstants.QOUTE_SINGLE_MARK);
					}

					params.append(value.toString());

					if (isString) {
						params.append(CoreConstants.QOUTE_SINGLE_MARK);
					}

					if (iter.hasNext()) {
						params.append(CoreConstants.COMMA).append(
								CoreConstants.SPACE);
					}
				}
				params.append(CoreConstants.BRACKET_RIGHT);
			}

			params.append(CoreConstants.SPACE);
			return params.toString();
		}
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByNames(List<String> names) {
		if (ListUtil.isEmpty(names)) {
			return null;
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(names, null, null);
		}

		String query = null;
		List<Serializable[]> data = null;
		try {
			query = getQuery(getSelectPart(getFullColumns()), getFromClause(), " where", getQueryParameters("var.NAME_", names, true),
					VAR_DEFAULT_CONDITION,
					getMirrowTableCondition());
			String maxLength = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("max_string_var_length");
			if (!StringUtil.isEmpty(maxLength))
				query = StringHandler.replace(query, "var.stringvalue_ as sv", "substr(var.stringvalue_, 1, " + maxLength + ") as sv");
			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables by names: " + names, e);
		}

		return getConverted(data, FULL_COLUMNS);
	}

	/**
	 * <p>Connects parts of query to MySQL query.</p>
	 * @param parts of MySQL query.
	 * @return MySQL query.
	 */
	private String getQuery(String... parts) {
		StringBuffer query = new StringBuffer();
		for (String part : parts) {
			query.append(part);
		}
		return query.toString();
	}

	public static final boolean isDataMirrowed() {
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("jbpm_variables_mirrowed", Boolean.FALSE);
	}

	private String getStringValueColumn(String prefix, String mirrowPrefix, boolean mirrow) {
		return mirrow ? mirrowPrefix.concat(".stringvalue") : prefix.concat(".stringvalue_");
	}

	private String getStringValueColumn(boolean mirrow) {
		return getStringValueColumn("var", MIRRROW_TABLE_ALIAS, mirrow);
	}

	private String getMirrowTableCondition(String varAlias, String mirrowAlias) {
		return getMirrowTableCondition(varAlias, mirrowAlias, true);
	}
	private String getMirrowTableCondition() {
		return getMirrowTableCondition(true);
	}
	private String getMirrowTableCondition(boolean full) {
		return getMirrowTableCondition("var.ID_", MIRRROW_TABLE_ALIAS, full);
	}
	private String getMirrowTableCondition(String varAlias, String mirrowAlias, boolean full) {
		return (full && isDataMirrowed()) ? " and " + varAlias + " = " + mirrowAlias + ".variable_id " : CoreConstants.EMPTY;
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

	@Override
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds) {
		return getFullVariablesByProcessInstanceIdsNaiveWay(processInstanceIds, null);
	}

	@Override
	public Collection<VariableInstanceInfo> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(existingVars, processInstanceIds);
		}

		int columns = COLUMNS + 2;
		List<Serializable[]> data = null;
		String query = getQuery("select ", STANDARD_COLUMNS, ", ", getSubstring("var.STRINGVALUE_"), " as sv, var.PROCESSINSTANCE_ ", FROM,
				" where ",
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

	/*@Override
	public void loadVariables(List<String> variablesNames) {
		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("No variables provided!");
			return;
		}

		for (String name: variablesNames) {
			try {
				List<String> tmp = Arrays.asList(name);
				Collection<VariableInstanceInfo> vars = getVariablesByNames(tmp);
				if (vars == null) {
					LOGGER.warning("No data was found for variable: " + tmp);
					return;
				}

				cachedVariablesNames.put(name, Boolean.TRUE);

				//	Name -> Value -> Variables
				Map<String, Map<String, List<VariableInstanceInfo>>> cache = getVariablesCache();
				for (VariableInstanceInfo var : vars) {
					String variableName = var.getName();
					if (StringUtil.isEmpty(variableName))
						continue;

					Serializable value = var.getValue();
					if (value == null)
						continue;

					Map<String, List<VariableInstanceInfo>> variablesByName = cache.get(variableName);
					if (variablesByName == null) {
						variablesByName = new HashMap<String, List<VariableInstanceInfo>>();
						cache.put(variableName, variablesByName);
					}

					String key = var instanceof VariableStringInstance ? ((String) value).intern() : value.toString().intern();
					List<VariableInstanceInfo> variablesByValues = variablesByName.get(key);
					if (variablesByValues == null) {
						variablesByValues = new ArrayList<VariableInstanceInfo>();
						variablesByName.put(key, variablesByValues);
					}
					variablesByValues.add(var);
				}
			} catch (Exception e) {
				String message = "Error loading variables by " + name + " into cache";
				LOGGER.log(Level.WARNING, message, e);
				CoreUtil.sendExceptionNotification(message, e);
				cachedVariablesNames.remove(name);
			}
		}
	}*/

	@Override
	public List<VariableInstanceInfo> getCachedVariables(String name) {
		return getCachedVariables(name, null, false, true);
	}

	@Override
	public List<VariableInstanceInfo> getCachedVariables(String name, Serializable value, boolean approximate) {
		return getCachedVariables(name, value, approximate, true);
	}

	@Override
	public Map<Long, VariableInstanceInfo> getCachedVariablesGroupedByProcess(String name) {
		//	Name -> Value -> Variable
		Map<String, Map<String, List<VariableInstanceInfo>>> cache = getVariablesCache();
		if (MapUtil.isEmpty(cache))
			return Collections.emptyMap();
		//	Value -> Variable
		Map<String, List<VariableInstanceInfo>> cachedVariables = cache.get(name);
		if (MapUtil.isEmpty(cachedVariables))
			return Collections.emptyMap();

		Map<Long, VariableInstanceInfo> allCachedVariables = new HashMap<Long, VariableInstanceInfo>();
		Collection<List<VariableInstanceInfo>> temp = new ArrayList<List<VariableInstanceInfo>>(cachedVariables.values());
		for (List<VariableInstanceInfo> varsForValue: temp) {
			List<VariableInstanceInfo> tempValues = new ArrayList<VariableInstanceInfo>(varsForValue);
			for (VariableInstanceInfo varForValue: tempValues)
				allCachedVariables.put(varForValue.getProcessInstanceId(), varForValue);
		}
		return allCachedVariables;
	}

	private List<VariableInstanceInfo> getCachedVariables(String name, Serializable value, boolean approximate, boolean allVariables) {
		if (StringUtil.isEmpty(name))
			return null;

		Map<String, VariableInstanceInfo> results = new HashMap<String, VariableInstanceInfo>();
		//	Name -> Value -> Variable
		Map<String, Map<String, List<VariableInstanceInfo>>> cache = getVariablesCache();
		//	Value -> Variable
		Map<String, List<VariableInstanceInfo>> cachedVariables = cache.get(name);
		if (MapUtil.isEmpty(cachedVariables))
			return Collections.emptyList();
		if (value == null && allVariables) {
			Map<Long, VariableInstanceInfo> allCachedVariables = getCachedVariablesGroupedByProcess(name);
			return new ArrayList<VariableInstanceInfo>(allCachedVariables.values());
		} else if (value == null)
			return Collections.emptyList();

		String valueKey = value.toString();
		//	All variables by provided name and value
		List<VariableInstanceInfo> cachedVariablesByNameAndValue = cachedVariables.get(valueKey);
		if (!approximate && ListUtil.isEmpty(cachedVariablesByNameAndValue)) {
			VariableInstanceInfo tmp = cachedVariables.values().iterator().next().iterator().next();
			if (tmp instanceof VariableStringInstance || tmp instanceof VariableDateInstance || tmp instanceof VariableLongInstance ||
					tmp instanceof VariableDoubleInstance) {
				LOGGER.warning("Provided value " + valueKey + " does not exist in cache. First variable in cache by name " + name + " is: " + tmp);
				return Collections.emptyList();
			} else if (tmp instanceof VariableByteArrayInstance) {
				valueKey = "[".concat(valueKey).concat("]");
				cachedVariablesByNameAndValue = cachedVariables.get(valueKey);
				if (ListUtil.isEmpty(cachedVariablesByNameAndValue)) {
					LOGGER.warning("Provided value " + valueKey + " does not exist in cache for byte array variables." +
							" First variable in cache by name " + name + " is: " + tmp);
					return Collections.emptyList();
				}
			} else {
				LOGGER.warning("Do not know how to resolve if provided value " + valueKey + " is cached for variables by name " + name +
						". First variable in cache: " + tmp);
				return Collections.emptyList();
			}
		}
		if (approximate && ListUtil.isEmpty(cachedVariablesByNameAndValue)) {
			Set<String> values = cachedVariables.keySet();
			cachedVariablesByNameAndValue = new ArrayList<VariableInstanceInfo>();
			for (String varValue: values) {
				if (varValue.toLowerCase().indexOf(valueKey.toLowerCase()) != -1)
					cachedVariablesByNameAndValue.addAll(cachedVariables.get(varValue));
			}
		}
		if (ListUtil.isEmpty(cachedVariablesByNameAndValue))
			return Collections.emptyList();

		VariableInstanceInfo variable = null;
		boolean iterCondition = allVariables ? Boolean.TRUE : variable == null;
		for (Iterator<VariableInstanceInfo> varsIterator = cachedVariablesByNameAndValue.iterator(); (varsIterator.hasNext() && iterCondition);) {
			VariableInstanceInfo var = varsIterator.next();
			if (var == null)
				continue;

			Serializable varValue = var.getValue();
			if (varValue == null)
				continue;

			String key = getKeyForVariable(var, false);
			if (results.get(key) != null)
				continue;

			boolean satisfiesCondition = false;
			if (var instanceof VariableStringInstance || var instanceof VariableLongInstance || var instanceof VariableDoubleInstance) {
				String realValue = valueKey;
				String realVarValue = var instanceof VariableStringInstance ? ((String) varValue).intern() : varValue.toString().intern();

				if (approximate) {
					realValue = realValue.toLowerCase();
					realVarValue = realVarValue.toLowerCase();
					satisfiesCondition = realVarValue.indexOf(realValue) != -1;
				} else {
					satisfiesCondition = valueKey.equals(varValue instanceof Number ? varValue.toString() : varValue);
				}
			} else if (var instanceof VariableDateInstance && ((Date) varValue).equals(value)) {
				variable = var;
				satisfiesCondition = true;
			} else if (var instanceof VariableByteArrayInstance) {
				satisfiesCondition = varValue.toString().intern().indexOf(valueKey) != -1;
			} else {
				LOGGER.warning("Do not know how to resovle if variable " + var + " satisfies condition: " + value +
						(approximate ? " approximately" : " strictly"));
			}

			variable = satisfiesCondition ? var : null;
			if (variable != null)
				results.put(key, variable);
		}

		return new ArrayList<VariableInstanceInfo>(results.values());
	}

	private VariableInstanceInfo getCachedVariable(String name, Serializable value) {
		List<VariableInstanceInfo> cached = getCachedVariables(name, value, false, false);
		return ListUtil.isEmpty(cached) ? null : cached.iterator().next();
	}

	private int lastUsedCacheSize = 5;
	private Map<String, Map<String, List<VariableInstanceInfo>>> getVariablesCache() {
		IWMainApplication iwma = IWMainApplication.getDefaultIWMainApplication();
		String cacheName = "bpmVariablesInfoCache";
		int cacheSize = Integer.valueOf(iwma.getSettings().getProperty("bpm_variables_cache_size", String.valueOf(5)));
		if (cacheSize == lastUsedCacheSize)
			return IWCacheManager2.getInstance(iwma).getCache(cacheName, cacheSize, true, false, 8640000, 8640000, false); // 100 days

		IWCacheManager2.getInstance(iwma).invalidate(cacheName);
		lastUsedCacheSize = cacheSize;
		return getVariablesCache();
	}

	@Override
	public void onApplicationEvent(final ApplicationEvent event) {
//			Thread importer = new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					VariableCreatedEvent varCreated = event;
//
//					Map<String, Object> vars = varCreated.getVariables();
//					if (MapUtil.isEmpty(vars)) {
//						getLogger().warning("No variables were submitted: " + varCreated);
//						return;
//					}
//
//					Long processInstanceId = varCreated.getProcessInstanceId();
//					Map<String, Map<String, List<VariableInstanceInfo>>> cache = getVariablesCache();
//					for (String name: vars.keySet()) {
//						if (!cachedVariablesNames.containsKey(name))
//							continue;	//	Variable is not cached
//
//						Map<String, List<VariableInstanceInfo>> cachedValues = cache.get(name);
//						if (cachedValues == null) {
//							cachedValues = new HashMap<String, List<VariableInstanceInfo>>();
//							cache.put(name, cachedValues);
//						}
//
//						VariableInstanceInfo info = getEmptyVariable(name);
//						if (info == null) {
//							getLogger().warning("Unable to add variable " + name + " to cache!");
//							continue;	//	Invalid variable
//						}
//
//						Object value = vars.get(name);
//						if (!(value instanceof Serializable)) {
//							if (value != null)
//								getLogger().warning("Invalid value (" + value + ", class: " + value.getClass().getName() + ") for variable " + name);
//
//							continue;	//	Invalid value
//						}
//
//						info.setProcessInstanceId(processInstanceId);
//						info.setValue((Serializable) value);
//
//						String key = value.toString();
//						List<VariableInstanceInfo> variablesByNameAndValue = cachedValues.get(key);
//						if (variablesByNameAndValue == null)
//							variablesByNameAndValue = new ArrayList<VariableInstanceInfo>();
//						variablesByNameAndValue.add(0, info);
//
//						cachedValues.put(key, variablesByNameAndValue);
//						cache.put(name, cachedValues);
//					}
//				}
//			});
//			importer.start();

		if (event instanceof VariableCreatedEvent) {
			final VariableCreatedEvent variableEvent = (VariableCreatedEvent) event;

			Thread binder = new Thread(new Runnable() {
				@Override
				public void run() {

					Map<String, Object> createdVariables = variableEvent.getVariables();
					bindProcessVariables(variableEvent.getProcessDefinitionName(), variableEvent.getProcessInstanceId(),
							createdVariables == null ? null : createdVariables.keySet());
				}
			});
			binder.start();

			doIndexVariables(variableEvent.getProcessInstanceId());
		} else if (event instanceof TaskInstanceSubmitted) {
			doIndexVariables(((TaskInstanceSubmitted) event).getProcessInstanceId());
		}
	}

	@Override
	@Transactional
	public void doIndexVariables(Long piId) {
		if (piId == null) {
			getLogger().warning("Process instance ID is not provided");
			return;
		}

		final List<Variable> variables = getResultList(
				Variable.QUERY_GET_BY_PROC_INST,
				Variable.class,
				new Param(Variable.PARAM_PROC_INST_ID, piId)
		);
		if (ListUtil.isEmpty(variables)) {
			getLogger().warning("Unable to index variables for proc. inst. " + piId + ": no variables found");
			return;
		}

		getBpmContext().execute(new JbpmCallback<Boolean>() {

			@Override
			public Boolean doInJbpm(JbpmContext context) throws JbpmException {
				FullTextSession indexer = org.hibernate.search.Search.getFullTextSession(context.getSession());
				Transaction tx = indexer.beginTransaction();
				for (Variable variable: variables) {
					if (variable.getValue() == null)
						continue;

					Object var = indexer.load(Variable.class, variable.getId());
					indexer.index(var);
				}
				tx.commit();

				return true;
			}
		});
	}

	@Override
	public void doBindProcessVariables() {
		if (!IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.bind_vars_with_proc_def", Boolean.FALSE)) {
			return;
		}

		try {
			List<AutoloadedProcessDefinition> procDefs = getAllLoadedProcessDefinitions();
			if (ListUtil.isEmpty(procDefs)) {
				return;
			}

			List<ProcessDefinitionVariablesBind> currentBinds = getAllProcDefVariableBinds();

			for (AutoloadedProcessDefinition apd: procDefs) {
				String processDefinitionName = apd.getProcessDefinitionName();
				Collection<VariableInstanceInfo> currentVariables = getVariablesByProcessDefinitionNaiveWay(processDefinitionName);
				bindProcessVariables(processDefinitionName, currentBinds, currentVariables, null);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error binding process variables", e);
		}
	}

	private void bindProcessVariables(String processDefinitionName, Long processInstanceId, Set<String> createdVariables) {
		bindProcessVariables(processDefinitionName, getAllProcDefVariableBinds(), getVariablesByProcessInstanceId(processInstanceId),
				createdVariables);
	}

	@Transactional(readOnly = false)
	private void bindProcessVariables(String processDefinitionName, List<ProcessDefinitionVariablesBind> currentBinds,
			Collection<VariableInstanceInfo> currentVariables, Set<String> createdVariables) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return;
		}

		currentBinds = currentBinds == null ? new ArrayList<ProcessDefinitionVariablesBind>() : currentBinds;

		if (ListUtil.isEmpty(currentVariables)) {
			if (ListUtil.isEmpty(createdVariables)) {
				return;
			}
			for (String variableName: createdVariables) {
				List<String> types = VariableInstanceType.getVariableTypeKeys(variableName);
				if (ListUtil.isEmpty(types)) {
					continue;
				}
				createProcessDefinitionVariablesBind(currentBinds, processDefinitionName, variableName, types.get(0));
			}
		} else {
			for (VariableInstanceInfo var: currentVariables) {
				String variableName = var.getName();
				if (StringUtil.isEmpty(variableName)) {
					continue;
				}

				createProcessDefinitionVariablesBind(currentBinds, processDefinitionName, variableName, var.getType().getTypeKeys().get(0));
			}
		}
	}

	private ProcessDefinitionVariablesBind createProcessDefinitionVariablesBind(
			List<ProcessDefinitionVariablesBind> currentBinds,
			String processDefinitionName,
			String variableName,
			String variableType
	) {
		try {
			if (bindExists(currentBinds, variableName, processDefinitionName)) {
				return null;
			}
			ProcessDefinitionVariablesBind bind = new ProcessDefinitionVariablesBind();
			if (bind.hashCode() < 0) {
				return null;
			}

			if (super.getEntityManager() == null) {
				super.setEntityManager(getEntityManager());
			}

			bind.setProcessDefinition(processDefinitionName);
			bind.setVariableName(variableName);
			bind.setVariableType(variableType);
			persist(bind);

			if (currentBinds == null)
				currentBinds = new ArrayList<ProcessDefinitionVariablesBind>();

			currentBinds.add(bind);
			LOGGER.info("Added new bind: " + bind);
			return bind;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error adding new bind: " + variableName + " for process: " + processDefinitionName, e);
		}
		return null;
	}

	private boolean bindExists(List<ProcessDefinitionVariablesBind> currentBinds, String variableName, String processDefinitionName) {
		if (ListUtil.isEmpty(currentBinds)) {
			return false;
		}

		String expression = variableName.concat("@").concat(processDefinitionName);
		for (ProcessDefinitionVariablesBind bind: currentBinds) {
			if (bind.toString().equals(expression)) {
				return true;
			}
		}
		return false;
	}

	private List<AutoloadedProcessDefinition> getAllLoadedProcessDefinitions() {
		try {
			return getResultList(AutoloadedProcessDefinition.QUERY_SELECT_ALL, AutoloadedProcessDefinition.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting auto loaded process definitions");
		}
		return null;
	}

	private List<ProcessDefinitionVariablesBind> getAllProcDefVariableBinds() {
		try {
			return getResultList(ProcessDefinitionVariablesBind.QUERY_SELECT_ALL, ProcessDefinitionVariablesBind.class);
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting variables for process definitions", e);
		}
		return null;
	}
	/* Variables binding ends **/

	@Override
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesByProcesses(
			Map<String, List<Serializable>> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables) {

		if (MapUtil.isEmpty(activeVariables)) {
			LOGGER.warning("There are no criterias provided for the variables, terminating data mining");
			return null;
		}

		Map<String, VariableQuerierData> queryParams = getConvertedData(activeVariables);
		return getVariablesByNamesAndValuesAndExpressionsByProcesses(queryParams, variables, procDefNames, procInstIds, flexibleVariables);
	}

	private Map<String, VariableQuerierData> getConvertedData(Map<String, List<Serializable>> activeVariables){
		if (MapUtil.isEmpty(activeVariables))
			return null;

		Map<String, VariableQuerierData> convertedVariables = new HashMap<String, VariableQuerierData>();

		for (String key: activeVariables.keySet())
			convertedVariables.put(key, new VariableQuerierData(key, activeVariables.get(key)));

		return convertedVariables;
	}

	@Override
	public Collection<VariableInstanceInfo> getVariablesByProcessInstanceIds(Collection<Long> procInstIds) {
		if (ListUtil.isEmpty(procInstIds))
			return null;

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(null, null, new ArrayList<Long>(procInstIds));
		}

		String query = getQuery(getSelectPart(STANDARD_COLUMNS), getFromClause(false, false), " where ", PROC_INST_IDS_EXPRESSION,
				" group by var.name_");
		return getVariablesByProcessInstanceIds(query, COLUMNS, new ArrayList<Long>(procInstIds));
	}

	@Override
	public List<VariableInstanceInfo> getVariablesByNameAndTaskInstance(final Collection<String> names, final Long tiId) {
		if (ListUtil.isEmpty(names) || tiId == null)
			return null;

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(null, new ArrayList<String>(names), null, null, Arrays.asList(tiId), null, null, false);
		}

		String query = getQuery(getSelectPart(STANDARD_COLUMNS), CoreConstants.COMMA, OTHER_VALUES, getFromClause(true, false),
				" where var.taskinstance_ = ",
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

	@Override
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

	@Override
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables) {

		return getVariablesByNamesAndValuesAndExpressionsByProcesses(activeVariables, variables, procDefNames, procInstIds,
				flexibleVariables, true);
	}

	@Override
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean useCachedVariables) {

		return getVariablesByNamesAndValuesAndExpressionsByProcesses(activeVariables, variables, procDefNames, procInstIds, flexibleVariables,
				useCachedVariables, false);
	}

	private EntityManager entityManager;

	@Override
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	protected EntityManager getEntityManager() {
		return entityManager;
	}

	private class ValueToMatch {
		private String field;
		private Object value;
		private Boolean flexible, multipleValues = Boolean.FALSE;
		private List<String> sentences;

		private ValueToMatch(String field, Object value, Boolean flexible, Boolean multipleValues, List<String> sentences) {
			this.field = field;
			this.value = value;
			this.flexible = flexible;
			this.multipleValues = multipleValues;
			this.sentences = sentences;

			if (sentences != null && sentences.size() > 1)
				multipleValues = true;
		}

		@Override
		public String toString() {
			return "Field: " + field + ", value to match: " + value + ", flexible: " + flexible + ", multiple values: " + multipleValues +
					", sentences: " + sentences;
		}
	}

	private ValueToMatch getValueToMatch(
			ValueToMatch previousValueToMatch,
			Serializable value, String name,
			VariableQuerierData var,
			Map<String, Boolean> flexibleVariables
	) {
		String field = null;
		Object valueToMatch = null;
		Boolean flexible = false;
		if (value instanceof String) {
			String stringValue = (String) value;
			stringValue = stringValue.trim();
			boolean foundNonStringValue = false;
			if (name.indexOf("string") == -1) {
				if (name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
						name.startsWith(VariableInstanceType.LIST.getPrefix()) ||
						name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix())) {
					field = "bytesValue";
					stringValue = StringHandler.replace(stringValue, CoreConstants.SPACE, CoreConstants.HASH);
					valueToMatch = CoreConstants.HASH + stringValue + CoreConstants.HASH;
					foundNonStringValue = true;
				} else if (stringValue.indexOf(CoreConstants.DOT) == -1 || stringValue.split(CoreConstants.DOT).length > 2) {
					//	Candidate for Long or Date
					Long longValue = null;
					try {
						longValue = Long.valueOf(stringValue);
					} catch (NumberFormatException e) {}
					if (longValue == null) {
						//	Checking if date
						Date dateValue = IWDatePickerHandler.getParsedDateByCurrentLocale(stringValue);
						if (dateValue != null) {
							field = "dateValue";
							valueToMatch = VariableDateInstanceBridge.DATE_VAR_FORMATTER.format(dateValue);
							foundNonStringValue = true;
							getLogger().info("Resolved value '" + dateValue + "' of Date type for " + var);
						}
					} else {
						field = "longValue";
						valueToMatch = longValue;
						foundNonStringValue = true;
						getLogger().info("Resolved value '" + longValue + "' of Long type for " + var);
					}
				} else {
					//	Candidate for double
					Double doubleValue = null;
					try {
						doubleValue = Double.valueOf(stringValue);
					} catch (NumberFormatException e) {}
					if (doubleValue != null) {
						field = "doubleValue";
						valueToMatch = doubleValue;
						foundNonStringValue = true;
						getLogger().info("Resolved value '" + doubleValue + "' of Double type for " + var);
					}
				}
			}

			if (!foundNonStringValue) {
				field = "stringValue";
				valueToMatch = stringValue;

				flexible = MapUtil.isEmpty(flexibleVariables) ? false : flexibleVariables.get(name);
				if (flexible == null) {
					flexible = false;
				}
				if (flexible) {
					if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.search_vars_full_like", Boolean.FALSE)) {
						valueToMatch = CoreConstants.STAR + valueToMatch.toString();
					}
					valueToMatch = valueToMatch.toString() + CoreConstants.STAR;
				}
			}
		} else if (value instanceof Double) {
			field = "doubleValue";
			valueToMatch = value;
		} else if (value instanceof Number) {
			field = "longValue";
			valueToMatch = ((Number) value).longValue();
		} else if (value instanceof Date) {
			field = "dateValue";
			valueToMatch = VariableDateInstanceBridge.DATE_VAR_FORMATTER.format((Date) value);
		}

		List<String> sentences = null;
		String sentence = null;
		if (field.equals("stringValue") &&
				(valueToMatch.toString().indexOf(CoreConstants.SPACE) != -1 ||
				(previousValueToMatch != null && !ListUtil.isEmpty(previousValueToMatch.sentences)))) {
			sentence = valueToMatch.toString();

			sentences = previousValueToMatch == null ? null : previousValueToMatch.sentences;
			if (sentences == null)
				sentences = new ArrayList<String>();
			sentences.add(sentence);
		}
		boolean multipleValues = false;
		if (previousValueToMatch != null && previousValueToMatch.value != null) {
			String currentValue = String.valueOf(valueToMatch);

			sentences = previousValueToMatch == null ? null : previousValueToMatch.sentences;
			if (sentences != null)
				sentences.add(currentValue);

			String previousValue = String.valueOf(previousValueToMatch.value);
			valueToMatch = previousValue.concat(CoreConstants.SPACE).concat(currentValue);
			multipleValues = true;
		}
		return new ValueToMatch(field, valueToMatch, flexible, multipleValues, sentences);
	}

	private BooleanJunction<?> getVariablesConditions(
			QueryBuilder query,
			VariableQuerierData activeVariable,
			List<String> names,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			Map<String, Boolean> flexibleVariables
	) {
		BooleanJunction<?> conditions = query.bool();

		if (activeVariable != null) {
			conditions.must(
					query.keyword()
						.onField("name")
						.matching(activeVariable.getName())
						.createQuery()
			);

			List<Serializable> values = activeVariable.getValues();
			if (!ListUtil.isEmpty(values)) {
				ValueToMatch valueToMatch = null;
				String name = activeVariable.getName();
				for (Serializable value: values) {
					valueToMatch = getValueToMatch(valueToMatch, value, name, activeVariable, flexibleVariables);
				}

				Termination<?> matching = null;
				if (valueToMatch.flexible) {
					matching = query.keyword()
									.wildcard()						//	To support LIKE
									.onField(valueToMatch.field)
									.matching(valueToMatch.value);
				} else {
					if (!ListUtil.isEmpty(valueToMatch.sentences)) {
						List<String> sentences = valueToMatch.sentences;
						if (sentences.size() == 1) {
							String sentence = sentences.get(0);
							matching = query.phrase()
												.onField(valueToMatch.field)
												.sentence(sentence);
							conditions.must(matching.createQuery());
						} else {
							BooleanJunction<?> sentencesCondition = query.bool();
							for (String sentence: sentences) {
								matching = query.phrase()
													.onField(valueToMatch.field)
													.sentence(sentence);
								sentencesCondition.should(matching.createQuery());
							}
							conditions.must(sentencesCondition.createQuery());
						}
						matching = null;
					} else if (valueToMatch.multipleValues) {
						matching = query.keyword()
											.onField(valueToMatch.field)
											.matching(valueToMatch.value);
					} else {
						matching = query.keyword()
											.onField(valueToMatch.field)
											.matching(valueToMatch.value);
					}
				}

				if (matching != null)
					conditions.must(matching.createQuery());
			}
		}

		if (!ListUtil.isEmpty(varIds)) {
			StringBuilder varIdsCond = new StringBuilder();
			for (Iterator<Long> varIdsIter = varIds.iterator(); varIdsIter.hasNext();) {
				varIdsCond.append(varIdsIter.next());
				if (varIdsIter.hasNext()) {
					varIdsCond.append(CoreConstants.SPACE);
				}
			}

			conditions.must(
					query.keyword()
							.onField("id")
							.matching(varIdsCond.toString())
							.createQuery()
			);
		}

		if (!ListUtil.isEmpty(procInstIds)) {
			StringBuilder procInstIdsCond = new StringBuilder();
			for (Iterator<Long> procInstIdsIter = procInstIds.iterator(); procInstIdsIter.hasNext();) {
				procInstIdsCond.append(procInstIdsIter.next());
				if (procInstIdsIter.hasNext()) {
					procInstIdsCond.append(CoreConstants.SPACE);
				}
			}

			conditions.must(
					query.keyword()
							.onField("processInstance")
							.matching(procInstIdsCond.toString())
							.createQuery()
			);
		}

		if (!ListUtil.isEmpty(taskInstIds)) {
			StringBuilder taskInstIdsCond = new StringBuilder();
			for (Iterator<Long> taskInstIdsIter = taskInstIds.iterator(); taskInstIdsIter.hasNext();) {
				taskInstIdsCond.append(taskInstIdsIter.next());
				if (taskInstIdsIter.hasNext()) {
					taskInstIdsCond.append(CoreConstants.SPACE);
				}
			}

			conditions.must(
					query.keyword()
							.onField("taskInstance")
							.matching(taskInstIdsCond.toString())
							.createQuery()
			);
		}

		if (activeVariable == null && !ListUtil.isEmpty(names)) {
			StringBuilder namesCond = new StringBuilder();
			for (Iterator<String> namesIter = names.iterator(); namesIter.hasNext();) {
				namesCond.append(namesIter.next());
				if (namesIter.hasNext()) {
					namesCond.append(CoreConstants.SPACE);
				}
			}

			conditions.must(
					query.keyword()
							.onField("name")
							.matching(namesCond.toString())
							.createQuery()
			);
		}

		return conditions;
	}

	private List<Variable> getVariablesByLucene(
			final VariableQuerierData queryData,
			final List<String> names,
			final List<Long> procInstIds,
			final List<Long> taskInstIds,
			final List<Long> varIds,
			final Map<String, Boolean> flexibleVariables
	) {
		long start = System.currentTimeMillis();

		boolean lucene = true;
		try {
			if (queryData == null && (!ListUtil.isEmpty(taskInstIds) || !ListUtil.isEmpty(varIds) || !ListUtil.isEmpty(procInstIds))) {
				lucene = false;
				return bpmDAO.getVariablesByConditions(names, procInstIds, taskInstIds, varIds);
			}

			List<Variable> data = getBpmContext().execute(new JbpmCallback<List<Variable>>() {

				@Override
				public List<Variable> doInJbpm(JbpmContext context) throws JbpmException {
					Session session = context.getSession();
					FullTextSession fullTextSession = Search.getFullTextSession(session);
					Transaction tx = fullTextSession.beginTransaction();

					QueryBuilder queryBuilder = fullTextSession
													.getSearchFactory()
													.buildQueryBuilder()
													.forEntity(Variable.class)
													.get();

					BooleanJunction<?> conditions = getVariablesConditions(
														queryBuilder,
														queryData,
														queryData == null ? names : null,
														procInstIds,
														taskInstIds,
														varIds,
														flexibleVariables
													);

					FullTextQuery query = fullTextSession.createFullTextQuery(conditions.createQuery());
					@SuppressWarnings("unchecked")
					List<Variable> results = query.list();

					tx.commit();

					return results;
				}
			});
			return data;
		} finally {
//			if (CoreUtil.isSQLMeasurementOn()) {
				long duration = System.currentTimeMillis() - start;
				if (duration >= 1000) {
					getLogger().info("******* " +  (lucene? "LUCENE" : "HIBERNATE") + ": it took " + duration + " ms to query for variable name(s): " +
							names + ", query: '" + queryData + "', proc. inst. IDs: " + procInstIds + ", task inst. IDs: " + taskInstIds +
							" and var. inst. IDs: " + varIds);
				}
//			}
		}
	}

	private List<VariableInstanceInfo> getVariablesCollectionByLucene(List<Long> varIds, List<Long> procInstIds) {
		return getVariablesCollectionByLucene(null, null, null, procInstIds, null, varIds, null, false);
	}

	private List<VariableInstanceInfo> getVariablesCollectionByLucene(List<String> variables, List<String> procDefNames, List<Long> procInstIds) {
		return getVariablesCollectionByLucene(null, variables, procDefNames, procInstIds, null, null, null, false);
	}

	private List<VariableInstanceInfo> getVariablesCollectionByLucene(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables
	) {
		Map<Long, Map<String, VariableInstanceInfo>> vars = getVariablesByLucene(
				activeVariables,
				variables,
				procDefNames,
				procInstIds,
				taskInstIds,
				varIds,
				flexibleVariables,
				strictBinaryVariables
		);

		List<VariableInstanceInfo> results = getConverted(vars);

		return results;
	}

	@Override
	public List<VariableInstanceInfo> getConverted(Map<Long, Map<String, VariableInstanceInfo>> vars) {
		if (MapUtil.isEmpty(vars)) {
			return null;
		}

		List<VariableInstanceInfo> results = new ArrayList<VariableInstanceInfo>();
		for (Map<String, VariableInstanceInfo> procVars: vars.values()) {
			results.addAll(procVars.values());
		}
		return results;
	}

	private Map<Long, Map<String, VariableInstanceInfo>> getVariablesByLucene(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables
	) {
		return getVariablesByLucene(activeVariables, variables, procDefNames, procInstIds, null, null, flexibleVariables, strictBinaryVariables);
	}

	private Map<Long, Map<String, VariableInstanceInfo>> getConverted(Collection<VariableInstanceInfo> vars) {
		if (ListUtil.isEmpty(vars))
			return null;

		Map<Long, Map<String, VariableInstanceInfo>> results = new HashMap<Long, Map<String,VariableInstanceInfo>>();
		for (VariableInstanceInfo var: vars) {
			Serializable value = var.getValue();
			if (value == null)
				continue;
			String name = var.getName();
			if (StringUtil.isEmpty(name))
				continue;

			Long procInstId = var.getProcessInstanceId();
			Map<String, VariableInstanceInfo> procInstData = results.get(procInstId);
			if (procInstData == null) {
				procInstData = new HashMap<String, VariableInstanceInfo>();
				results.put(procInstId, procInstData);
			}

			procInstData.put(name, var);
		}
		return results;
	}

	private Map<Long, Map<String, VariableInstanceInfo>> getVariablesByLucene(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables
	) {
		try {
			if (variables != null)
				variables = new ArrayList<String>(variables);

			Map<Long, Map<String, VariableInstanceInfo>> results = null;

			if (MapUtil.isEmpty(activeVariables)) {
				List<Variable> vars = getVariablesByLucene(null, variables, procInstIds, taskInstIds, varIds, flexibleVariables);
				if (ListUtil.isEmpty(vars)) {
					getLogger().warning("Nothing found from Hibernate by var. names: " + variables + ", proc. inst. IDs: " + procInstIds +
							", task inst. IDs: " + taskInstIds + ", var. inst. IDs: " + varIds);
					return null;
				}

				results = getConvertedVariables(vars);
			} else {
				if (!ListUtil.isEmpty(procInstIds) || !ListUtil.isEmpty(taskInstIds) || !ListUtil.isEmpty(varIds)) {
					List<Serializable[]> data = getInformationByVariablesNameAndValuesAndProcesses(null, null, null,
							new ArrayList<VariableQuerierData>(activeVariables.values()),
							variables, null,
							procInstIds, taskInstIds, varIds, true, false, false);
					Collection<VariableInstanceInfo> vars = getConverted(data, FULL_COLUMNS);
					results = getConverted(vars);
					if (MapUtil.isEmpty(results)) {
						getLogger().warning("Nothing found from database by query " + activeVariables + " var. names: " + variables +
								", proc. inst. IDs: " + procInstIds + ", task inst. IDs: " + taskInstIds + ", var. inst. IDs: " + varIds);
					}
					return results;
				}

				//	1. Find proc. inst. IDs from variables by conditions
				List<Variable> vars = null;
				boolean loadAddtionalVars = !ListUtil.isEmpty(variables);
				Map<Long, Boolean> allFoundedProcInstIds = null;
				Map<Long, List<Variable>> allVariables = null;
				for (final VariableQuerierData queryData: activeVariables.values()) {
					List<Variable> tmpVars = getVariablesByLucene(queryData, variables, procInstIds, taskInstIds, varIds, flexibleVariables);
					if (ListUtil.isEmpty(tmpVars)) {
						getLogger().warning("Nothing found from Lucene by " + queryData + ". Terminating search");
						return null;
					}

					Map<Long, Boolean> foundedProcInstIds = new HashMap<Long, Boolean>();
					for (Variable var: tmpVars) {
						Long piId = var.getProcessInstance();
						foundedProcInstIds.put(piId, Boolean.TRUE);

						if (loadAddtionalVars) {
							if (variables == null)
								variables = new ArrayList<String>();
							if (!variables.contains(queryData.getName()))
								variables.add(queryData.getName());
						} else {
							if (allVariables == null)
								allVariables = new HashMap<Long, List<Variable>>();

							List<Variable> varsByProcInst = allVariables.get(piId);
							if (varsByProcInst == null) {
								varsByProcInst = new ArrayList<Variable>();
								allVariables.put(piId, varsByProcInst);
							}
							varsByProcInst.add(var);
						}
					}

					boolean firstHit = allFoundedProcInstIds == null;
					if (firstHit) {
						allFoundedProcInstIds = new HashMap<Long, Boolean>(foundedProcInstIds);
					} else {
						allFoundedProcInstIds.keySet().retainAll(foundedProcInstIds.keySet());
					}
				}

				//	2. Making sure only requested data will be returned
				if (MapUtil.isEmpty(allFoundedProcInstIds))
					return null;

				if (!ListUtil.isEmpty(procInstIds))
					allFoundedProcInstIds.keySet().retainAll(procInstIds);

				if (MapUtil.isEmpty(allFoundedProcInstIds))
					return null;

				//	3. Find rest of variables by eligible proc. inst. IDs
				if (loadAddtionalVars) {
					vars = getVariablesByLucene(null, variables, new ArrayList<Long>(allFoundedProcInstIds.keySet()), taskInstIds, varIds, null);
				} else {
					allVariables.keySet().retainAll(allFoundedProcInstIds.keySet());
					vars = new ArrayList<Variable>();
					for (Collection<Variable> procInstVars: allVariables.values()) {
						vars.addAll(procInstVars);
					}
				}

				if (ListUtil.isEmpty(vars))
					return null;

				results = getConvertedVariables(vars);
				if (MapUtil.isEmpty(results)) {
					getLogger().warning("Unable to convert " + vars);
					return null;
				}
			}

			if (!ListUtil.isEmpty(procInstIds) && !MapUtil.isEmpty(results)) {
				//	Making sure results are correct
				results.keySet().retainAll(procInstIds);
			}

			return results;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error querying for " + variables + " by " + activeVariables + " and proc. inst. IDs: " + procInstIds, e);
		}

		return null;
	}

	@Override
	public Map<Long, Map<String, VariableInstanceInfo>> getConvertedVariables(List<Variable> variables) {
		if (ListUtil.isEmpty(variables))
			return Collections.emptyMap();

		Map<Number, VariableByteArrayInstance> variablesToConvert = new HashMap<Number, VariableByteArrayInstance>();
		Map<Long, Map<String, VariableInstanceInfo>> converted = new HashMap<Long, Map<String,VariableInstanceInfo>>();
		for (Variable variable: variables) {
			Long piId = variable.getProcessInstance();
			if (piId == null)
				continue;

			Map<String, VariableInstanceInfo> procInstData = converted.get(piId);
			if (procInstData == null) {
				procInstData = new HashMap<String, VariableInstanceInfo>();
				converted.put(piId, procInstData);
			}

			String name = variable.getName();
			if (StringUtil.isEmpty(name))
				continue;

			Character type = variable.getClassType();
			Serializable value = variable.getValue();
			if (value == null)
				continue;

			VariableInstanceInfo var = getVariable(
					variable.getId(),
					name,
					value,
					type == null ? null : String.valueOf(type),
					variablesToConvert
			);
			VariableInstanceInfo existingVar = procInstData.get(name);
			if (existingVar == null) {
				existingVar = var;
			}

			if (variable.getTaskInstance() != null) {
				if (existingVar.getId() == null)
					existingVar.setId(variable.getId());

				if (variable.getId() != null && variable.getId() >= existingVar.getId()) {
					existingVar.setId(variable.getId());
					existingVar.setValue(value);
				}
				existingVar.setProcessInstanceId(variable.getProcessInstance());
				existingVar.setTaskInstanceId(variable.getTaskInstance());
			}
			procInstData.put(name, existingVar);
		}

		convertByteValues(variablesToConvert);
		if (!MapUtil.isEmpty(variablesToConvert)) {
			getLogger().info("Convert: " + variablesToConvert);
//		for (VariableByteArrayInstance byteVar: variablesToConvert.values())
//			variables.put(byteVar.getId(), byteVar);
		}

		return converted;
	}

	private boolean isLuceneQueryingTurnedOn() {
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("use_lucene_for_bpm_search", Boolean.TRUE);
	}

	@Override
	public Map<Long, Map<String, VariableInstanceInfo>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> originalProcInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean useCachedVariables,
			boolean strictBinaryVariables) {

		if (!MapUtil.isEmpty(activeVariables)) {
			List<VariableQuerierData> queryData = new ArrayList<VariableQuerierData>(activeVariables.values());
			Collections.sort(queryData, new Comparator<VariableQuerierData>() {

				@Override
				public int compare(VariableQuerierData var1, VariableQuerierData var2) {
					return -1 * (Integer.valueOf(var1.getOrder()).compareTo(var2.getOrder()));
				}
			});

			activeVariables = new LinkedHashMap<String, VariableQuerierData>();
			for (VariableQuerierData activeVar: queryData)
				activeVariables.put(activeVar.getName(), activeVar);
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesByLucene(
					activeVariables,
					variables,
					procDefNames,
					originalProcInstIds,
					flexibleVariables,
					strictBinaryVariables
			);
		}

		List<String> variablesToQuery = new ArrayList<String>();
		List<Long> procInstIds = originalProcInstIds == null ? null : new ArrayList<Long>(originalProcInstIds);

		//	Determining cached variables
		Map<String, List<VariableInstanceInfo>> cachedVariables = new HashMap<String, List<VariableInstanceInfo>>();
		if (!MapUtil.isEmpty(activeVariables)) {
			if (useCachedVariables) {
				for (String name: activeVariables.keySet()) {
					List<Serializable> values = activeVariables.get(name).getValues();
					for (Serializable value: values) {
						List<VariableInstanceInfo> info = getCachedVariables(
								name, value,
								flexibleVariables == null ?
										false : flexibleVariables.get(name) == null
										? false : flexibleVariables.get(name), true);
						if (ListUtil.isEmpty(info)) {
							if (isVariableCached(name)) {
								LOGGER.warning("Variable " + name + " is cached, but no variables were found by value: " + value);
								return null;
							} else
								continue;
						}

						List<VariableInstanceInfo> existingInfo = cachedVariables.get(name);
						if (existingInfo != null) {
							existingInfo.addAll(info);
							cachedVariables.put(name, existingInfo);
						} else
							cachedVariables.put(name, info);
					}

					List<VariableInstanceInfo> cached = cachedVariables.get(name);
					if (cached == null || cached.size() < activeVariables.get(name).getValues().size()) {
						if (!variablesToQuery.contains(name))
							variablesToQuery.add(name);

						cachedVariables.remove(name);
					}
				}
			} else
				variablesToQuery.addAll(activeVariables.keySet());
		}
		int numberOfCachedVariables = cachedVariables.size();

		Map<Long, Map<String, VariableInstanceInfo>> results = new LinkedHashMap<Long, Map<String, VariableInstanceInfo>>();

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

			List<Long> idsToRemove = new ArrayList<Long>();
			for (Long id: results.keySet()) {
				Map<String, VariableInstanceInfo> cachedData = results.get(id);
				if (MapUtil.isEmpty(cachedData) || cachedData.keySet().size() != numberOfCachedVariables)
					idsToRemove.add(id);
			}
			for (Long id: idsToRemove)
				results.remove(id);

			if (numberOfCachedVariables == activeVariables.size() && MapUtil.isEmpty(results))
				return null;

			if (ListUtil.isEmpty(procInstIds)) {
				if (MapUtil.isEmpty(results))
					return null;	//	Nothing was found in the cache

				//	Must query by process instance IDs that were found by cached variables
				procInstIds = new ArrayList<Long>(results.keySet());
			} else {
				//	Restricting a set of process instance IDs
				procInstIds.retainAll(results.keySet());
				if (ListUtil.isEmpty(procInstIds)) {
					LOGGER.info("There are no results after initial data set was restricted by the cached data " + results.keySet());
					return Collections.emptyMap();
				}
			}
		}

		if (useCachedVariables && ListUtil.isEmpty(variablesToQuery) && ListUtil.isEmpty(variables)) {
			//	Everything was found in cache or nothing else needed to query
			return results;
		} else if (!MapUtil.isEmpty(activeVariables)) {
			for (String variableName: activeVariables.keySet()) {
				if (variablesToQuery.contains(variableName))
					continue;

				if (useCachedVariables && cachedVariables.containsKey(variableName))
					continue;

				variablesToQuery.add(variableName);
			}
		}

		Map<String, VariableQuerierData> variablesAndValuesToQuery = new HashMap<String, VariableQuerierData>();
		for (String variableToQuery: variablesToQuery)
			variablesAndValuesToQuery.put(variableToQuery, activeVariables.get(variableToQuery));

		//	Querying the DB for variables
		Collection<VariableInstanceInfo> info = getProcessVariablesByNamesAndValues(variablesAndValuesToQuery, variables,
				 ListUtil.isEmpty(procInstIds) ? procDefNames : null, procInstIds, true, flexibleVariables, strictBinaryVariables);

		if (ListUtil.isEmpty(info))
			return null;

		//	Making copy of cached process instance IDs
		List<Long> cachedProcInstIds = MapUtil.isEmpty(results) ? null : new ArrayList<Long>(results.keySet());
		//	Marking process instance IDs of queried data
		Map<Long, Boolean> queriedIds = new HashMap<Long, Boolean>();

		//	Combining cached results and queried results
		for (VariableInstanceInfo varInfo: info) {
			if (varInfo == null)
				continue;
			Long piId = varInfo.getProcessInstanceId();
			if (!ListUtil.isEmpty(originalProcInstIds) && !originalProcInstIds.contains(piId))
				continue;

			queriedIds.put(piId, Boolean.TRUE);

			//	Getting variables of a process
			Map<String, VariableInstanceInfo> processVariables = results.get(piId);
			if (processVariables == null) {
				processVariables = new HashMap<String, VariableInstanceInfo>();
				results.put(piId, processVariables);
			}

			if (varInfo.getValue() != null) {
				VariableInstanceInfo existingValue = processVariables.get(varInfo.getName());
				if (existingValue == null) {
					processVariables.put(varInfo.getName(), varInfo);
				} else {
					Long varId = varInfo.getId();
					Long existingId = existingValue.getId();
					if (varId != null && existingId != null && varId > existingId)
						processVariables.put(varInfo.getName(), varInfo);
				}
			}
		}

		if (!MapUtil.isEmpty(variablesAndValuesToQuery) && !MapUtil.isEmpty(queriedIds) && !ListUtil.isEmpty(cachedProcInstIds)) {
			List<Long> idsToRemove = new ArrayList<Long>();

			//	Checking if anything was found by criteria(s) in DB that was found in cache by other criteria(s)
			for (Long piId: cachedProcInstIds) {
				if (!queriedIds.containsKey(piId))
					idsToRemove.add(piId);
			}

			if (!ListUtil.isEmpty(idsToRemove))
				results.keySet().removeAll(idsToRemove);
		} else {
			if (!ListUtil.isEmpty(originalProcInstIds)) {
				results.keySet().retainAll(originalProcInstIds);
			}
		}

		return results;
	}

}