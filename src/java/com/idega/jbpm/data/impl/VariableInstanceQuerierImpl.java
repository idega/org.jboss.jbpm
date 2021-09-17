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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import com.idega.bpm.model.VariableInstance;
import com.idega.builder.bean.AdvancedProperty;
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
import com.idega.jbpm.data.VariableBytes;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.event.ProcessInstanceCreatedEvent;
import com.idega.jbpm.event.TaskInstanceSubmitted;
import com.idega.jbpm.event.VariableCreatedEvent;
import com.idega.jbpm.exe.ProcessDefinitionW;
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
										" var.TASKINSTANCE_ as tiid, var.PROCESSINSTANCE_ as piid",
								MIRRROW_TABLE_ALIAS = "mirrow",
								PROC_INST_IDS_EXPRESSION = ":processInstanceIdsExpression",
								TASK_INST_IDS_EXPRESSION = ":taskInstanceIdsExpression";

	private static final int	COLUMNS = 3,
								FULL_COLUMNS = COLUMNS + 7;

	private static final Random ID_GENERATOR = new Random();

	private String getSelectPart(String columns) {
		return getSelectPart(columns, Boolean.FALSE);
	}

	private String getSelectPart(String columns, boolean distinct) {
		return "select ".concat(distinct ? "distinct " : CoreConstants.EMPTY).concat(columns);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName, false);
	}

	private <V extends VariableInstance> Collection<V> getVariablesByProcessDefinitionNaiveWay(String processDefinitionName, boolean full) {
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

		return getConverted(data);
	}

	@Override
	public Collection<VariableInstance> getVariablesByProcessDefinition(String processDefinitionName) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			LOGGER.warning("Process definition name is not provided");
			return null;
		}

		List<ProcessDefinitionVariablesBind> binds = null;
		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm_variable_load_from_binds", true)) {
			binds = getResultList(
			    ProcessDefinitionVariablesBind.QUERY_SELECT_BY_PROCESS_DEFINITION_NAMES,
			    ProcessDefinitionVariablesBind.class,
			    new Param(ProcessDefinitionVariablesBind.PARAM_PROC_DEF_NAMES, Arrays.asList(processDefinitionName))
			);
		}
		if (!ListUtil.isEmpty(binds)) {
			return getConvertedBinds(binds);
		}

		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getFullVariablesByProcessDefinition(String processDefinitionName) {
		return getVariablesByProcessDefinitionNaiveWay(processDefinitionName, true);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getFullVariablesByProcessInstanceId(Long processInstanceId, boolean mirrow) {
		return getVariablesByProcessInstanceId(processInstanceId, true, mirrow);
	}

	private <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceId(Long processInstanceId, boolean full, boolean mirrow) {
		return getVariablesByInstanceId(processInstanceId, full, mirrow, false);
	}
	private <V extends VariableInstance> Collection<V> getVariablesByInstanceId(Long id, boolean full, boolean mirrow, boolean taskInstance) {
		String typeOfId = taskInstance ? "task" : "process";
		if (id == null) {
			LOGGER.warning("Invalid ID of " + typeOfId + " instance");
			return null;
		}

		String query = null;
		List<Serializable[]> data = null;
		try {
			String selectColumns = full ? getFullColumns(mirrow, false) : STANDARD_COLUMNS;
			int columns = full ? FULL_COLUMNS : COLUMNS;
			query = getQuery(getSelectPart(selectColumns), getFromClause(mirrow), " where var." + (taskInstance ? "TASKINSTANCE_" : "PROCESSINSTANCE_") + " = ",
					String.valueOf(id), VAR_DEFAULT_CONDITION, getMirrowTableCondition(mirrow));
			data = SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variable instances by " + typeOfId + " instance: " + id, e);
		}

		return getConverted(data);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getFullVariablesByTaskInstanceId(Long taskInstanceId) {
		return getVariablesByInstanceId(taskInstanceId, true, isDataMirrowed(), true);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, false, isDataMirrowed());
	}

	@Override
	public <V extends VariableInstance> Collection<V> getFullVariablesByProcessInstanceId(Long processInstanceId) {
		return getVariablesByProcessInstanceId(processInstanceId, true, isDataMirrowed());
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names) {
		return getVariablesByProcessesAndVariablesNames(procIds, null, names, isDataMirrowed(), false);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getFullVariablesByTaskInstanceIdsAndVariablesNames(List<Long> taskInstanceIds, List<String> names) {
		return getVariablesByInstancesAndVariablesNames(taskInstanceIds, null, names, isDataMirrowed(), false, true, false);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariableByProcessInstanceIdAndVariableName(Long procId, String name) {
		if (procId == null || StringUtil.isEmpty(name)) {
			return null;
		}
		return getVariablesByProcessesAndVariablesNames(Arrays.asList(procId), null, Arrays.asList(name), isDataMirrowed(), false);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(Collection<Long> procIds, List<String> names,
			boolean checkTaskInstance) {
		return getVariablesByProcessesAndVariablesNames(procIds, null, names, isDataMirrowed(), checkTaskInstance);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessDefsAndVariablesNames(List<String> procDefs, List<String> names) {
		return getVariablesByProcessesAndVariablesNames(null, procDefs, names, isDataMirrowed(), true);
	}
	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessDefAndVariableName(String procDefName, String name) {
		return getVariablesByProcessDefAndVariableName(procDefName, name, false);
	}
	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessDefAndVariableName(String procDefName, String name, boolean checkBind) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}
		return getVariablesByProcessDefAndVariablesNames(procDefName, Arrays.asList(name), checkBind);
	}
	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessDefAndVariablesNames(String procDefName, List<String> names, boolean checkBind) {
		if (StringUtil.isEmpty(procDefName) || ListUtil.isEmpty(names)) {
			return null;
		}
		return getVariablesByProcessesAndVariablesNames(
				names,
				Arrays.asList(procDefName),
				null,
				false,
				true,
				isDataMirrowed(),
				checkBind
		);
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.VariableInstanceQuerier#getVariables(java.util.List, java.util.List, boolean)
	 */
	@Override
	public <V extends VariableInstance> Collection<V> getVariables(List<String> procDefs, List<String> names, boolean checkTaskInstance) {
		return getVariablesByProcessesAndVariablesNames(names, procDefs, null, checkTaskInstance, Boolean.FALSE, isDataMirrowed(), false);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds,
			boolean checkTaskInstance, boolean addEmptyVars) {
		return getVariablesByProcessInstanceIdAndVariablesNames(names, procIds, checkTaskInstance, addEmptyVars, isDataMirrowed());
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceIdAndVariablesNames(List<String> names, Collection<Long> procIds,
			boolean checkTaskInstance, boolean addEmptyVars, boolean mirrowData) {

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(names, null, ListUtil.isEmpty(procIds) ? null : new ArrayList<>(procIds));
		}

		if (ListUtil.isEmpty(procIds)) {
			LOGGER.warning("Process instance(s) unkown");
			return null;
		}

		return getVariablesByProcessesAndVariablesNames(names, null, procIds, checkTaskInstance, addEmptyVars, mirrowData, false);
	}

	private <V extends VariableInstance> Collection<V> getVariablesByProcessesAndVariablesNames(
			List<String> variablesNames,
			List<String> procDefNames,
			Collection<Long> procInstIds,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData,
			boolean checkBind
	) {
		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("Variable name(s) unknown");
			return Collections.emptyList();
		}

		Map<Integer, V> vars = new HashMap<>();
		if (mirrowData) {
			List<String> notStringVariables = new ArrayList<>();
			for (String name: variablesNames) {
				if (!name.startsWith(VariableInstanceType.STRING.getPrefix()) && !ProcessDefinitionW.VARIABLE_MANAGER_ROLE_NAME.equals(name)) {
					notStringVariables.add(name);
				}
			}
			List<String> stringVariables = new ArrayList<>(variablesNames);
			if (!ListUtil.isEmpty(notStringVariables)) {
				stringVariables.removeAll(notStringVariables);
			}

			Collection<V> stringVars = getVariablesByProcessesAndVariablesNames(
					procInstIds,
					procDefNames,
					stringVariables,
					mirrowData,
					checkTaskInstance,
					checkBind
			);
			Collection<V> otherVars = getVariablesByProcessesAndVariablesNames(
					procInstIds,
					procDefNames,
					notStringVariables,
					false,
					checkTaskInstance,
					checkBind
			);

			fillMapWithData(vars, variablesNames, stringVars);
			fillMapWithData(vars, variablesNames, otherVars);
		} else {
			Collection<V> allVars = getVariablesByProcessesAndVariablesNames(
					procInstIds,
					procDefNames,
					variablesNames,
					mirrowData,
					checkTaskInstance,
					checkBind
			);
			fillMapWithData(vars, variablesNames, allVars);
		}

		List<V> variables = null;
		if (vars.size() > 0) {
			variables = new ArrayList<>();
			if (addEmptyVars) {
				for (int i = 0; i < variablesNames.size(); i++) {
					V info = vars.get(i);
					if (info == null) {
						info = getEmptyVariable(variablesNames.get(i));
						if (info == null) {
							throw new RuntimeException();
						} else {
							Serializable id = info.getProcessInstanceId();
							if (id instanceof Long) {
								Long procInstId = (Long) id;
								int key = getKey(i, procInstId);
								vars.put(key, info);
							}
						}
					}
				}
			}

			List<Integer> keys = new ArrayList<>(vars.keySet());
			Collections.sort(keys);
			for (Integer key : keys) {
				variables.add(vars.get(key));
			}
		}

		return variables;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.VariableInstanceQuerier#getVariables(java.util.Collection, java.util.Collection, boolean, boolean, boolean)
	 */
	@Override
	public <V extends VariableInstance> List<V> getVariables(
			Collection<String> variableNames,
			Collection<ProcessInstance> processInstances,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	) {
		Set<Long> processInstanceIDs = null;
		if (!ListUtil.isEmpty(processInstances)) {
			processInstanceIDs = new HashSet<>(processInstances.size());

			for (ProcessInstance pi : processInstances) {
				processInstanceIDs.add(pi.getId());
			}
		}

		return getVariables(variableNames, processInstanceIDs, checkTaskInstance, addEmptyVars, mirrowData);
	}

	@Override
	public <V extends VariableInstance> List<V> getVariables(
			Collection<String> variableNames,
			Set<Long> processInstancesIds,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	) {
		List<String> names = null;
		if (!ListUtil.isEmpty(variableNames)) {
			names = new ArrayList<>(variableNames);
		}

		Collection<V> variableInstances = getVariablesByProcessInstanceIdAndVariablesNames(names, processInstancesIds, checkTaskInstance, addEmptyVars, mirrowData);
		if (ListUtil.isEmpty(variableInstances)) {
			return Collections.emptyList();
		}

		return new ArrayList<>(variableInstances);
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.VariableInstanceQuerier#getSortedVariables(java.util.Collection, java.util.Collection, boolean, boolean, boolean)
	 */
	@Override
	public <V extends VariableInstance> Map<String, V> getSortedVariables(
			Collection<String> variableNames,
			Collection<ProcessInstance> processInstances,
			boolean checkTaskInstance,
			boolean addEmptyVars,
			boolean mirrowData
	) {
		Map<String, V> map = new HashMap<>();

		List<V> unsortedVariables = getVariables(
				variableNames, processInstances, checkTaskInstance,
				addEmptyVars, mirrowData);
		for (V unsortedVariable : unsortedVariables) {
			map.put(unsortedVariable.getName(), unsortedVariable);
		}

		return map;
	}

	private <V extends VariableInstance> V getEmptyVariable(String name) {
		if (name.startsWith(VariableInstanceType.STRING.getPrefix()) || name.endsWith("officerIsMeterMaid") || ProcessDefinitionW.VARIABLE_MANAGER_ROLE_NAME.equals(name)) {
			@SuppressWarnings("unchecked")
			V result = (V) new VariableStringInstance(name, null);
			return result;

		} else if (name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
					name.startsWith(VariableInstanceType.LIST.getPrefix()) ||
					name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix())) {
			@SuppressWarnings("unchecked")
			V result = (V) new VariableByteArrayInstance(name, null);
			return result;

		} else if (name.startsWith(VariableInstanceType.DATE.getPrefix())) {
			@SuppressWarnings("unchecked")
			V result = (V) new VariableDateInstance(name, null);
			return result;

		} else if (name.startsWith(VariableInstanceType.DOUBLE.getPrefix())) {
			@SuppressWarnings("unchecked")
			V result = (V) new VariableDoubleInstance(name, null);
			return result;

		} else if (name.startsWith(VariableInstanceType.LONG.getPrefix()) || name.equals("officerID")) {
			@SuppressWarnings("unchecked")
			V result = (V) new VariableLongInstance(name, null);
			return result;

		} else {
			LOGGER.warning("Can not resolve variable type from name: " + name + ". Will use default one");
			@SuppressWarnings("unchecked")
			V result = (V) VariableInstanceInfo.getDefaultVariable(name);
			return result;
		}
	}

	private <V extends VariableInstance> void fillMapWithData(Map<Integer, V> vars, List<String> names, Collection<V> variables) {
		if (ListUtil.isEmpty(variables)) {
			return;
		}

		List<String> addedVars = new ArrayList<>();
		for (V var : variables) {
			String name = var.getName();
			Serializable id = var.getProcessInstanceId();
			if (id instanceof Long) {
				Long processInstance = (Long) id;
				int key = getKey(names.indexOf(name), processInstance);
				if (processInstance != null && addedVars.contains(name + processInstance)) {
					V addedVar = vars.get(key);
					if (addedVar != null && addedVar.getVariableValue() != null)
					 {
						continue;	//	Added variable has value, it's OK
					}
				} else {
					addedVars.add(name + processInstance);
				}

				vars.put(key, var);
			}
		}
	}

	private int getKey(int index, Long piId) {
		return piId == null ? index : index * 100000000 + piId.intValue();
	}

	@Override
	public List<String> getValuesByVariable(String name) {
		if (StringUtil.isEmpty(name)) {
			return null;
		}

		if (isLuceneQueryingTurnedOn()) {
			List<VariableInstanceInfo> vars = getVariablesCollectionByLucene(Arrays.asList(name), null, null);
			if (ListUtil.isEmpty(vars)) {
				return null;
			}

			Map<String, Boolean> values = new HashMap<>();
			for (VariableInstanceInfo var: vars) {
				Serializable value = var.getValue();
				if (value instanceof String) {
					values.put((String) value, Boolean.TRUE);
				}
			}
			return new ArrayList<>(values.keySet());
		}

		List<Serializable[]> data = null;
		String query = "select distinct v.stringvalue_ from JBPM_VARIABLEINSTANCE v where v.NAME_ = '" + name +
			"' order by v.stringvalue_";
		try {
			data = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(data)) {
			return null;
		}

		List<String> results = new ArrayList<>();
		for (Serializable[] value: data) {
			if (ArrayUtil.isEmpty(value) || value[0] == null) {
				continue;
			}

			results.add(value[0].toString());
		}
		return results;
	}

	private <V extends VariableInstance> Collection<V> getVariablesByProcessesAndVariablesNames(
			Collection<Long> procInstIds,
			List<String> procDefNames,
			List<String> variablesNames,
			boolean mirrow,
			boolean checkTaskInstance
	) {
		return getVariablesByProcessesAndVariablesNames(procInstIds, procDefNames, variablesNames, mirrow, checkTaskInstance, false);
	}
	private <V extends VariableInstance> Collection<V> getVariablesByProcessesAndVariablesNames(
			Collection<Long> procInstIds,
			List<String> procDefNames,
			List<String> variablesNames,
			boolean mirrow,
			boolean checkTaskInstance,
			boolean checkBind
	) {
		return getVariablesByInstancesAndVariablesNames(procInstIds, procDefNames, variablesNames, mirrow, checkTaskInstance, false, checkBind);
	}
	private <V extends VariableInstance> Collection<V> getVariablesByInstancesAndVariablesNames(
			Collection<Long> instIds,
			List<String> procDefNames,
			List<String> variablesNames,
			boolean mirrow,
			boolean checkTaskInstance,
			boolean taskInstances,
			boolean checkBind
	) {
		if (isLuceneQueryingTurnedOn() && ListUtil.isEmpty(procDefNames) && !taskInstances) {
			return getVariablesCollectionByLucene(variablesNames, procDefNames, ListUtil.isEmpty(instIds) ? null : new ArrayList<>(instIds));
		}

		if (ListUtil.isEmpty(variablesNames)) {
			LOGGER.warning("The names of variables are not provided!");
			return null;
		}
		if (ListUtil.isEmpty(instIds) && ListUtil.isEmpty(procDefNames)) {
			LOGGER.warning((taskInstances ? "Task" : "Proc.") + " inst. IDs or proc. def. names must be provided!");
			return null;
		}

		variablesNames = new ArrayList<>(variablesNames);
		List<V> allVariables = new ArrayList<>();

		boolean anyStringColumn = false;
		for (Iterator<String> namesIter = variablesNames.iterator(); (!anyStringColumn && namesIter.hasNext());) {
			String name = namesIter.next();
			anyStringColumn = 	name.contains(VariableInstanceType.STRING.getPrefix()) ||
								ProcessDefinitionW.VARIABLE_MANAGER_ROLE_NAME.equals(name);
		}

		Collection<V> vars = null;

		boolean byProcInstIds = !ListUtil.isEmpty(instIds) && !taskInstances;

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
					byProcInstIds || taskInstances ?
							CoreConstants.EMPTY :
							", JBPM_PROCESSINSTANCE pi, JBPM_PROCESSDEFINITION pd ",
					checkBind ?
							", BPM_CASES_PROCESSINSTANCES cp " :
							CoreConstants.EMPTY,
					" where ",
					byProcInstIds ?
							PROC_INST_IDS_EXPRESSION :
							taskInstances ?
									TASK_INST_IDS_EXPRESSION :
									getQueryParameters("pd.name_", procDefNames, true) + " and pd.ID_ = pi.PROCESSDEFINITION_ and pi.ID_ = var.PROCESSINSTANCE_ ",
					checkBind ?
							" and pi.id_ = cp.process_instance_id " :
							CoreConstants.EMPTY,
					mirrow ?
							getMirrowTableCondition(true) :
							CoreConstants.EMPTY,
					checkTaskInstance ?
							" and var.TASKINSTANCE_ = task.ID_ and task.END_ is not null " :
							CoreConstants.EMPTY,
					" and " + varNamesIn, " and ", CLASS_CONDITION + " order by var.TASKINSTANCE_");
			vars = byProcInstIds || taskInstances ?
					getVariablesByInstanceIds(query, columns, new ArrayList<>(instIds), taskInstances ? TASK_INST_IDS_EXPRESSION : PROC_INST_IDS_EXPRESSION) :
					getVariablesByQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables for " + (taskInstances ? "task" : "process") + " instance(s) : " + instIds +
					" and name(s): "
					+ variablesNames, e);
		}

		if (ListUtil.isEmpty(allVariables)) {
			return vars;
		}

		if (!ListUtil.isEmpty(vars)) {
			allVariables.addAll(vars);
		}

		return allVariables;
	}

	@Override
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

	@Override
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValue(String name, Serializable value) {
		return getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(name, value, null);
	}

	@Override
	public Collection<Long> getProcessInstanceIdsByVariableNameAndValueAndProcInstIds(String name, Serializable value, List<Long> procInstIds) {
		Collection<VariableInstanceInfo> variables = null;
		try {
			variables = getProcessVariablesByNameAndValue(name, Arrays.asList(value), null, procInstIds, true, false, isDataMirrowed());
		} catch (Exception e) {
			String message = "Error getting variable '" + name + "', by value '" + value + "' and proc. inst. IDs: " + procInstIds;
			LOGGER.log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}
		if (ListUtil.isEmpty(variables)) {
			getLogger().warning("No variables found by name " + name + " and value " + value + (ListUtil.isEmpty(procInstIds) ? CoreConstants.EMPTY : " and proc. inst. IDs " + procInstIds));
			return null;
		}

		Collection<Long> piIds = new ArrayList<>();
		for (VariableInstanceInfo variable : variables) {
			Long piId = null;
			if (value instanceof String) {
				if (isValueEquivalent(variable, value)) {
					Serializable id = variable.getProcessInstanceId();
					piId = id instanceof Long ? (Long) id : null;
				}
			} else {
				Serializable id = variable.getProcessInstanceId();
				piId = id instanceof Long ? (Long) id : null;
			}

			if (piId != null && !piIds.contains(piId)) {
				piIds.add(piId);
			}
		}

		return piIds;
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByNameAndValue(String name, Serializable value) {
		Collection<V> variables = getProcessVariablesByNameAndValue(name, value, false);
		if (ListUtil.isEmpty(variables) || !(value instanceof String)) {
			return variables;
		}

		Collection<V> filtered = new ArrayList<>();
		for (V variable : variables) {
			if (isValueEquivalent(variable, value)) {
				filtered.add(variable);
			}
		}

		return filtered;
	}

	private <V extends VariableInstance> boolean isValueEquivalent(V variable, Serializable value) {
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

	private <V extends VariableInstance> Collection<V> getProcessVariablesByNameAndValue(String name, Serializable value, boolean selectProcessInstanceId) {
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
		if (value == null) {
			return null;
		}

		StringBuilder expression = new StringBuilder();
		boolean searchExpression = flexible && !StringUtil.isEmpty(searchSymbol);

		if (!searchExpression) {
			searchSymbol = CoreConstants.EQ;
		}

		if (value instanceof String) {
			expression.append(searchExpression ? " lower(" : CoreConstants.EMPTY)
			.append(columnName)
			.append(searchExpression ? ")" : CoreConstants.EMPTY)
			.append(searchExpression || isLikeExpressionEnabled() ? " like " : " = ")
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
		if (bpmContext == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmContext;
	}

	private <V extends VariableInstance> Collection<V> getVariablesByNameAndValuesAndProcessDefinitions(
			String name,
			List<String> procDefNames,
			Collection<?> values,
			List<Long> procInstIds
	) {
		Map<Long, BinaryVariableData<V>> data = getDataByVariablesByNameAndValuesAndProcessDefinitions(name, procDefNames, values, procInstIds);
		if (MapUtil.isEmpty(data)) {
			return null;
		}

		List<V> vars = new ArrayList<>();
		for (BinaryVariableData<V> binVarData: data.values()) {
			vars.add(binVarData.getVariable());
		}
		return vars;
	}

	class BinaryVariableData<V extends VariableInstance> {
		private Long procInstId;
		private Serializable[] rawData;
		private V variable;

		BinaryVariableData(Long procInstId, Serializable[] rawData, V variable) {
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

		V getVariable() {
			return variable;
		}

		@Override
		public String toString() {
			return getProcessInstanceId() + ": " + getVariable() + ". Raw data: " + getRawData();
		}
	}

	private <V extends VariableInstance> Map<Long, BinaryVariableData<V>> getDataByVariablesByNameAndValuesAndProcessDefinitions(
			String name,
			List<String> procDefNames,
			Collection<?> values,
			List<Long> procInstIds
	) {
		if (StringUtil.isEmpty(name)) {
			return Collections.emptyMap();
		}

		int columns = COLUMNS + 3;
		boolean byProcInst = !ListUtil.isEmpty(procInstIds);
		//	Getting all binary variables
		String query = getQuery(getSelectPart(STANDARD_COLUMNS), ", b.BYTES_, var.taskinstance_, var.PROCESSINSTANCE_ ", getFromClause(true, false),
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
				getDataByInstanceIds(null, query, columns, PROC_INST_IDS_EXPRESSION, procInstIds) :
				getDataByQuery(query, columns);
		if (ListUtil.isEmpty(data)) {
			return Collections.emptyMap();
		}

		Collection<V> resolvedVars = null;
		Collection<V> vars = getConverted(data);
		MultipleSelectionVariablesResolver resolver = getMultipleValuesResolver(name);
		if (resolver == null) {
			if (ListUtil.isEmpty(vars)) {
				return Collections.emptyMap();
			}

			List<String> addedVars = new ArrayList<>();
			Collection<V> uniqueVars = new ArrayList<>();
			for (V var: vars) {
				String key = getKeyForVariable(var, false);
				if (addedVars.contains(key)) {
					continue;
				}

				Object value = var.getVariableValue();
				if (value instanceof Collection<?>) {
					Collection<?> realValue = (Collection<?>) value;
					if (!CollectionUtils.isEqualCollection(realValue, values)) {
						continue;
					}

					uniqueVars.add(var);
					addedVars.add(key);
				}
			}

			resolvedVars = uniqueVars;
		} else {
			resolvedVars = getResolvedVariables(resolver, vars, values);
		}

		if (ListUtil.isEmpty(resolvedVars)) {
			return Collections.emptyMap();
		}

		Map<Long, V> resolvedProcInstIds = new HashMap<>();
		Map<Long, BinaryVariableData<V>> results = new HashMap<>();
		for (Serializable[] rawData: data) {
			if (ArrayUtil.isEmpty(rawData)) {
				continue;
			}

			Long procInstId = null;
			Serializable id = rawData[rawData.length - 1];
			if (id instanceof Number) {
				procInstId = ((Number) id).longValue();
			}
			if (procInstId == null) {
				continue;
			}

			if (resolvedProcInstIds.size() == 0) {
				for (V var: resolvedVars) {
					Serializable instId = var.getProcessInstanceId();
					if (instId instanceof Long) {
						resolvedProcInstIds.put((Long) instId, var);
					}
				}
			}

			if (!resolvedProcInstIds.containsKey(procInstId)) {
				continue;
			}

			results.put(procInstId, new BinaryVariableData<>(procInstId, rawData, resolvedProcInstIds.get(procInstId)));
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

	private <V extends VariableInstance> Collection<V> getResolvedVariables(
			MultipleSelectionVariablesResolver resolver,
			Collection<V> vars,
			Collection<?> values
	) {
		Collection<AdvancedProperty> resolvedValues = resolver == null ? null : resolver.getBinaryVariablesValues(vars, values);
		if (ListUtil.isEmpty(resolvedValues)) {
			return null;
		}

		List<String> addedVars = new ArrayList<>();
		Collection<V> resolvedVars = new ArrayList<>();

		//	Checking if resolver provided final results
		Collection<V> finalResult = resolver == null ? null : resolver.getFinalSearchResult();
		if (!ListUtil.isEmpty(finalResult)) {
			for (V var: finalResult) {
				Serializable id = var.getProcessInstanceId();
				String key = getKeyForVariable(var, true).concat(String.valueOf(id));
				if (!addedVars.contains(key)) {
					resolvedVars.add(var);
					addedVars.add(key);
				}
			}
			return resolvedVars;
		}

		//	Final results were not provided - trying to find the variables satisfying user query
		List<String> resolvedIds = new ArrayList<>();
		for (AdvancedProperty resolved: resolvedValues) {
			for (Object value: values) {
				if (value instanceof Serializable && resolved.getId().equals(value.toString()) && !resolvedIds.contains(value.toString())) {
					resolvedIds.add(value.toString());
				}
			}
		}
		for (String resolvedId: resolvedIds) {
			for (V var: vars) {
				if (var.getVariableValue().toString().indexOf(resolvedId) != -1) {
					Serializable procInstId = var.getProcessInstanceId();
					String key = getKeyForVariable(var, true).concat(String.valueOf(procInstId));
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
	public <V extends VariableInstance> Collection<V> getProcessVariablesByNameAndValue(String name, List<Serializable> values, List<String> procDefNames) {
		return getProcessVariablesByNameAndValue(name, values, procDefNames, null, true, true, isDataMirrowed());
	}

	@Override
	public <V extends VariableInstance> Collection<V> getProcessVariablesByNameAndValue(
			String name,
			List<Serializable> values,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean searchExpression,
			boolean mirrow) {

		VariableQuerierData dataList = new VariableQuerierData(name, 0, searchExpression, values);

		return getProcessVariablesByNameAndValue(name, dataList, procDefNames, procInstIds, selectProcessInstanceId, mirrow);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getProcessVariablesByNameAndValue(
			String name,
			VariableQuerierData data,
			List<String> procDefNames,
			List<Long> procInstIds,
			boolean selectProcessInstanceId,
			boolean mirrow) {

		if (StringUtil.isEmpty(name) || data == null) {
			return Collections.emptyList();
		}

		List<Serializable> values = data.getValues();
		if (values instanceof Collection<?>	&& (
				name.startsWith(VariableInstanceType.LIST.getPrefix()) ||
				name.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
				name.startsWith(VariableInstanceType.OBJ_LIST.getPrefix()))) {
			//	Binary data, need specific handling
			return getVariablesByNameAndValuesAndProcessDefinitions(name, procDefNames, values, procInstIds);
		}

		return getProcessVariablesByNamesAndValues(Arrays.asList(data), null, procDefNames, procInstIds, selectProcessInstanceId, mirrow, false);
	}

	private <V extends VariableInstance> Collection<V> getProcessVariablesByNamesAndValues(
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
				activeVariables = new HashMap<>();
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

		List<Serializable[]> queriedData = getInformationByVariablesNameAndValuesAndProcesses(
				data,
				variablesWithoutValues,
				procDefNames,
				procInstIds,
				selectProcessInstanceId,
				mirrow,
				strictBinaryVariables
		);
		if (ListUtil.isEmpty(queriedData)) {
			return Collections.emptyList();
		}

		//	Converting raw data into the objects
		return getConverted(queriedData);
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

	private List<Long> getNarrowedResults(String prefix, String keyword, List<Long> initialIds, List<Long> resultIds) {
		if (ListUtil.isEmpty(initialIds)) {
			return resultIds;
		}

		if (resultIds == null) {
			resultIds = new ArrayList<>();
		}

		List<Long> usedIds = null;
		if (initialIds.size() > 1000) {
			usedIds = new ArrayList<>(initialIds.subList(0, 1000));
			initialIds = new ArrayList<>(initialIds.subList(1000, initialIds.size()));
		} else {
			usedIds = new ArrayList<>(initialIds);
			initialIds = null;
		}

		List<Serializable[]> results = null;
		keyword = keyword.trim();
		if (!keyword.startsWith("and ")) {
			keyword = " and " + keyword;
		}
		String query = "select " + prefix + ".processinstance_ from jbpm_variableinstance " + prefix + " where " +
				getQueryParameters(prefix + ".processinstance_", usedIds, false) + keyword + " group by " + prefix + ".processinstance_";
		try {
			results = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error executing query: " + query, e);
			return null;
		}

		if (!ListUtil.isEmpty(results)) {
			for (Serializable[] ids: results) {
				if (ArrayUtil.isEmpty(ids)) {
					continue;
				}

				Serializable id = ids[0];
				if (id instanceof Number) {
					resultIds.add(((Number) id).longValue());
				}
			}
		}

		return getNarrowedResults(prefix, keyword, initialIds, resultIds);
	}

	private <V extends VariableInstance> List<Serializable[]> getInformationByVariablesNameAndValuesAndProcesses(
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
		Map<String, String> mirrows = new HashMap<>();
		Map<String, String> fromParts = new HashMap<>();
		Map<String, VariableInstanceType> types = new HashMap<>();

		boolean searchForEachKeywordSeparately = IWMainApplication.getDefaultIWMainApplication().getSettings()
				.getBoolean("bpm.var_load_res_separ", Boolean.TRUE);

		if (!ListUtil.isEmpty(data)) {
			boolean addAnd = false;
			for (Iterator<VariableQuerierData> queryPartIter = data.iterator(); queryPartIter.hasNext();) {
				VariableQuerierData queryPart = queryPartIter.next();

				String variableName = queryPart.getName();
				VariableInstanceType type = queryPart.getType();

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

				if (type == null) {
					if (value instanceof String) {
						type = VariableInstanceType.STRING;
					} else if (value instanceof Long) {
						type = VariableInstanceType.LONG;
					} else if (value instanceof Double) {
						type = VariableInstanceType.DOUBLE;
					} else if (value instanceof Timestamp) {
						type = VariableInstanceType.DATE;
					}
				}
				if (type == null) {
					getLogger().warning("Unknown variable (name: " + variableName + ") type, using String as default");
					type = VariableInstanceType.STRING;
				}

				if (currentColumnPrefix == null) {
					if (data.size() == 1 && ListUtil.isEmpty(variablesWithoutValues)) {
						useBinding = false;
						currentColumnPrefix = columnPrefix;
					} else {
						currentColumnPrefix = "v".concat(currentColumnPrefix == null ? columnPrefix : currentColumnPrefix);
					}
				} else {
					currentColumnPrefix = "v".concat(currentColumnPrefix);
				}

				String currentColumnName = currentColumnPrefix.concat(CoreConstants.DOT);

				if (values instanceof Collection<?>	&& (
						variableName.startsWith(VariableInstanceType.LIST.getPrefix()) ||
						variableName.startsWith(VariableInstanceType.BYTE_ARRAY.getPrefix()) ||
						variableName.startsWith(VariableInstanceType.OBJ_LIST.getPrefix()))
				) {
					//	Binary data, need specific handling
					Map<Long, BinaryVariableData<V>> binaryData = getDataByVariablesByNameAndValuesAndProcessDefinitions(
							variableName,
							procDefNames,
							queryPart.getValues(),
							procInstIds
					);
					if (MapUtil.isEmpty(binaryData)) {
						LOGGER.warning("No variables were found by name " + variableName + ", values " + queryPart.getValues() +
								", proc. def. names " + procDefNames + " and proc. inst. IDs " + procInstIds);
						return Collections.emptyList();
					}

					if (binaryResults == null) {
						binaryResults = new HashMap<>();
					}

					for (BinaryVariableData<V> binVarData: binaryData.values()) {
						Long procInstId = binVarData.getProcessInstanceId();
						List<Serializable[]> rawData = binaryResults.get(procInstId);
						if (rawData == null) {
							rawData = new ArrayList<>();
							binaryResults.put(procInstId, rawData);
						}
						rawData.add(binVarData.getRawData());
					}

					addAnd = false;
					continue;
				} else if (type == VariableInstanceType.STRING) {
					anyStringColumn = true;
					currentColumnName = getStringValueColumn(currentColumnPrefix, mirrowPrefix, mirrow);
					currentColumnName = mirrow ? currentColumnName : getSubstring(currentColumnName);
					types.put(currentColumnPrefix, VariableInstanceType.STRING);
					mirrows.put(currentColumnPrefix, mirrowPrefix);
					mirrowPrefix = "m".concat(mirrowPrefix);
				} else if (type == VariableInstanceType.LONG) {
					currentColumnName = currentColumnName.concat("LONGVALUE_");
					types.put(currentColumnPrefix, VariableInstanceType.LONG);
				} else if (type == VariableInstanceType.DOUBLE) {
					currentColumnName = currentColumnName.concat("DOUBLEVALUE_");
					types.put(currentColumnPrefix, VariableInstanceType.DOUBLE);
				} else if (type == VariableInstanceType.DATE) {
					currentColumnName = currentColumnName.concat("DATEVALUE_");
					types.put(currentColumnPrefix, VariableInstanceType.DATE);
				} else {
					LOGGER.warning("Unsupported type of value: " + value + ", "	+ value.getClass());
					return Collections.emptyList();
				}

				if (addAnd) {
					valuesToSelect.append(" and ");
				}

				if (values.size() > 1) {
					//	Multiple values
					if (currentColumnName.indexOf("string") == -1) {
						//	Not string values
						valuesToSelect.append(currentColumnName).append(" in (");
						for (Iterator<Serializable> valuesIter = values.iterator();	valuesIter.hasNext();) {
							valuesToSelect.append(valuesIter.next());

							if (valuesIter.hasNext()) {
								valuesToSelect.append(", ");
							}
						}
						valuesToSelect.append(") ");
					} else {
						//	String values
						valuesToSelect.append("(");
						for (Iterator<Serializable> valuesIter = values.iterator(); valuesIter.hasNext();) {
							valuesToSelect.append(getColumnAndValueExpression(currentColumnName, valuesIter.next(), queryPart.getSearchExpression(),
									queryPart.isFlexible()));

							if (valuesIter.hasNext()) {
								valuesToSelect.append(" or ");
							}
						}
						valuesToSelect.append(")");
					}
				} else {
					//	Single value
					valuesToSelect.append(getColumnAndValueExpression(currentColumnName, values.get(0), queryPart.getSearchExpression(), queryPart.isFlexible()));
				}

				valuesToSelect.append(" and ").append(currentColumnPrefix).append(".name_ = '").append(variableName).append("' ");

				if (variablesWithoutValues == null) {
					variablesWithoutValues = new ArrayList<>();
				} else {
					variablesWithoutValues = new ArrayList<>(variablesWithoutValues);
				}
				if (!variablesWithoutValues.contains(variableName)) {
					variablesWithoutValues.add(variableName);
				}

				fromParts.put(currentColumnPrefix, currentColumnName);

				if (searchForEachKeywordSeparately && !ListUtil.isEmpty(procInstIds)) {
					procInstIds = getNarrowedResults(currentColumnPrefix, valuesToSelect.toString(), procInstIds, null);
					if (ListUtil.isEmpty(procInstIds)) {
						getLogger().info("Nothing was found while narrowing results by variable value " + currentColumnPrefix + " = " + valuesToSelect +
								" and proc. inst. IDs " + procInstIds + ". Terminating search");
						return null;
					}

					fromParts.remove(currentColumnPrefix);
					types.remove(currentColumnPrefix);
					valuesToSelect = new StringBuffer();
				}

				addAnd = true;
			}
		}
		allValuesClause = valuesToSelect.toString();
		if (!StringUtil.isEmpty(allValuesClause)) {
			allValuesClause = " and ".concat(allValuesClause);
		}

		List<Long> tmpIds = null;
		if (binaryResults == null || !strictBinaryVariables) {
			tmpIds = procInstIds;
		} else {
			tmpIds = new ArrayList<>(binaryResults.keySet());
		}
		boolean byProcessInstances = !ListUtil.isEmpty(tmpIds);
		boolean byProcDefs = !ListUtil.isEmpty(procDefNames);

		String query = null;
		int columns = COLUMNS + 6;
		columns = numberOfColumns == null ? (columns + (selectProcessInstanceId ? 1 : 0)) : numberOfColumns;
		String select = null;
		if (StringUtil.isEmpty(columnsToSelect)) {
			select = getSelectPart(STANDARD_COLUMNS + ", " + getSubstring(getStringValueColumn(false)) +
					", var.LONGVALUE_ as lv, var.DOUBLEVALUE_ as dov, var.DATEVALUE_ as dav, var.BYTEARRAYVALUE_ as bv, var.TASKINSTANCE_ tiid ", false);
			select = select.concat(selectProcessInstanceId || byProcessInstances ? ", var.PROCESSINSTANCE_ as piid" : CoreConstants.EMPTY);
		} else {
			select = columnsToSelect;
		}

		List<String> parts = new ArrayList<>(Arrays.asList(select, " from JBPM_VARIABLEINSTANCE var ",
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

				if (keysIter.hasNext()) {
					parts.add(CoreConstants.COMMA.concat(CoreConstants.SPACE));
				}
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
			if (byProcessInstances || byProcDefs) {
				parts.add(" and ");
			}
			parts.add(getQueryParameters(columnName.concat("name_"), variablesWithoutValues, true));
		}

		if (bindingExpression != null) {
			parts.add(bindingExpression.toString());
		}

		if (!MapUtil.isEmpty(types)) {
			if (byProcDefs || byProcessInstances) {
				parts.add(" and ");
			}
			for (Iterator<Map.Entry<String, VariableInstanceType>> typesIter = types.entrySet().iterator(); typesIter.hasNext();) {
				Map.Entry<String, VariableInstanceType> type = typesIter.next();
				parts.add(getQueryParameters(type.getKey().concat(".class_"), type.getValue().getTypeKeys(), true));
				if (typesIter.hasNext()) {
					parts.add(" and ");
				}
			}
		}
		parts.add(" and var.class_ <> 'N' ");

		parts.add(allValuesClause);
		if (anyStringColumn && mirrow) {
			for (Map.Entry<String, String> mirrowEntry: mirrows.entrySet()) {
				parts.add(getMirrowTableCondition(mirrowEntry.getKey().concat(".id_"), mirrowEntry.getValue()));
			}
		}
		if (!StringUtil.isEmpty(groupBy)) {
			parts.add(" ".concat(groupBy));
		}
		parts.add(" order by var.TASKINSTANCE_");
		query = getQuery(ArrayUtil.convertListToArray(parts));

		List<Serializable[]> results = byProcessInstances ?
				getDataByInstanceIds(null, query, columns, PROC_INST_IDS_EXPRESSION, tmpIds) :
				getDataByQuery(query, columns);

		if (ListUtil.isEmpty(results) && binaryResults != null) {
			//	Returning just binary variables if any were found
			results = new ArrayList<>();
			for (List<Serializable[]> rawData: binaryResults.values()) {
				results.addAll(rawData);
			}
			return results;
		}

		if (!MapUtil.isEmpty(binaryResults)) {
			//	Combining binary variables with other variables
			for (List<Serializable[]> rawData: binaryResults.values()) {
				results.addAll(rawData);
			}
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
	private <V extends VariableInstance> Collection<V> getVariablesByInstanceIds(
			String query,
			int columns,
			List<Long> instIds,
			String idsExpression
	) {
		List<Serializable[]> data = getDataByInstanceIds(null, query, columns, idsExpression, instIds);
		return getConverted(data);
	}

	private List<Serializable[]> getDataByInstanceIds(List<Serializable[]> data, String query, int columns, String expression, List<Long> instIds) {
		if (data == null) {
			data = new ArrayList<>();
		}
		if (ListUtil.isEmpty(instIds)) {
			return data;
		}

		List<Long> usedIds = null;
		if (instIds.size() > 1000) {
			usedIds = new ArrayList<>(instIds.subList(0, 1000));
			instIds = new ArrayList<>(instIds.subList(1000,	instIds.size()));
		} else {
			usedIds = new ArrayList<>(instIds);
			instIds = null;
		}

		List<Serializable[]> queriedData = getDataByQuery(
				StringHandler.replace(query, expression, getQueryParameters("var." + (expression.equals(PROC_INST_IDS_EXPRESSION) ? "PROCESSINSTANCE" : "TASKINSTANCE") + "_", usedIds, false)),
				columns
		);

		if (queriedData != null) {
			data.addAll(queriedData);
		}

		return getDataByInstanceIds(data, query, columns, expression, instIds);
	}

	/**
	 * <p>Queries database. Converts database data to {@link List} of
	 * {@link VariableInstance}.</p>
	 * @param query to database.
	 * @param columns number selected from database in response.
	 * @return {@link Collection} of {@link VariableInstance} or
	 * <code>null</code> on failure;
	 */
	private <V extends VariableInstance> Collection<V> getVariablesByQuery(String query, int columns) {
		return getConverted(getDataByQuery(query, columns));
	}

	private List<Serializable[]> getDataByQuery(String query, int columns) {
		boolean measure = CoreUtil.isSQLMeasurementOn();
		long start = measure ? System.currentTimeMillis() : 0;
		try {
			return SimpleQuerier.executeQuery(query, columns);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'", e);
		} finally {
			if (measure) {
				CoreUtil.doDebugSQL(start, System.currentTimeMillis(), query);
			}
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
	 * @return {@link Collection} of {@link VariableInstance} or
	 * <code>null</code> on failure.
	 */
	private <V extends VariableInstance> Collection<V> getProcessVariablesByNamesAndValues(
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

		List<VariableQuerierData> data = new ArrayList<>(namesAndValues.values());

		if (!MapUtil.isEmpty(flexibleVariables)) {
			for (VariableQuerierData queryItem: data) {
				Boolean flexible = flexibleVariables.get(queryItem.getName());
				if (flexible == null) {
					flexible = Boolean.FALSE;
				}

				if (flexible) {
					queryItem.setSearchExpression(" like ");
				}

				queryItem.setFlexible(flexible);
			}
		}

		Collection<V> results = getProcessVariablesByNamesAndValues(
				data,
				variables,
				procDefNames,
				procInstIds,
				selectProcessInstanceId,
				isDataMirrowed(),
				strictBinaryVariables
		);

		return results;
	}

	private String getKeyForVariable(VariableInstance var, boolean useValue) {
		Serializable procInstId = var.getProcessInstanceId();
		StringBuilder key = new StringBuilder(var.getName()).append(procInstId.toString());
		if (useValue) {
			Serializable value = var.getVariableValue();
			key.append(value);
		}
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

	private <V extends VariableInstance> Collection<V> getConvertedBinds(Collection<ProcessDefinitionVariablesBind> binds) {
		if (ListUtil.isEmpty(binds)) {
			return null;
		}

		List<String> variables = new ArrayList<>();
		List<Serializable[]> data = new ArrayList<>();
		for (ProcessDefinitionVariablesBind bind: binds) {
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

		return getConverted(data);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getConverted(List<Serializable[]> data) {
		if (ListUtil.isEmpty(data)) {
			return null;
		}

		boolean sort = true;

		Map<Number, V> variables = new HashMap<>();
		Map<Number, VariableByteArrayInstance> variablesToConvert = new HashMap<>();
		for (Serializable[] dataSet: data) {
			if (ArrayUtil.isEmpty(dataSet)) {
				continue;
			}

			int numberOfColumns = dataSet.length;
			int startValues = 0;

			Number id = null;
			String name = null;
			try {
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
					LOGGER.warning("Unable to create variable from initial values (" + dataSet + "): ID: " + id	+ ", name: " + name	+ ", class: " +
							type);
					continue;
				}

				Serializable value = null;
				Long piId = null, tiId = null;
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
					if (numberOfColumns == FULL_COLUMNS) {
						piIdIndex = numberOfColumns - 1;
					} else if (numberOfColumns == FULL_COLUMNS + 1) {
						piIdIndex = numberOfColumns - 2;
					} else {
						piIdIndex = dataSet.length - 1;
					}
					if (piIdIndex >= index && piIdIndex < dataSet.length) {
						Serializable temp = dataSet[piIdIndex];
						if (temp instanceof Number) {
							piId = Long.valueOf(((Number) temp).longValue());
						}

						int tiIdIndex = piIdIndex - 1;
						if (tiIdIndex >= index && tiIdIndex < dataSet.length) {
							Serializable tempTiId = dataSet[tiIdIndex];
							if (tempTiId instanceof Number) {
								tiId = Long.valueOf(((Number) tempTiId).longValue());
							}
						}
					}

					//	Resolving case
					if (numberOfColumns > FULL_COLUMNS) {
						Serializable tmp = dataSet[dataSet.length - 1];
						if (tmp instanceof Number) {
							caseId = String.valueOf(((Number) tmp).longValue());
						}
					}
				}

				V variable = variables.get(id);
				if (variable == null) {
					@SuppressWarnings("unchecked")
					V convertedVariable = (V) getVariable(id, name, value, type, variablesToConvert);
					if (convertedVariable != null) {
						variable = convertedVariable;
					}

					if (variable == null || !(variable instanceof VariableInstanceInfo)) {
						LOGGER.warning("Unkown variable with id: " + id + ", name: '" + name + "', type: '" + type + "' and value: " + value);
					} else {
						VariableInstanceInfo vii = (VariableInstanceInfo) variable;
						vii.setId(id.longValue());
						vii.setProcessInstanceId(piId);
						vii.setTaskInstanceId(tiId);
						if (caseId != null) {
							vii.setCaseId(caseId);
						}
						@SuppressWarnings("unchecked")
						V filledInVariable = (V) vii;
						variables.put(id, filledInVariable);
					}
				} else if (value != null) {
					variable.setVariableValue(value);
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error converting SQL results into variable. ID: " + id + ", name: " + name, e);
			}
		}

		convertByteValues(variablesToConvert);
		for (VariableByteArrayInstance byteVar: variablesToConvert.values()) {
			@SuppressWarnings("unchecked")
			V temp = (V) byteVar;
			variables.put(byteVar.getId(), temp);
		}

		if (sort) {
			List<V> vars = new ArrayList<>(variables.values());
			Collections.sort(vars, new VariableInstanceInfoComparator());
			return vars;
		}

		return variables.values();
	}

	private <V extends VariableInstance> V getVariable(
			Number id,
			String name,
			Serializable value,
			String type,
			Map<Number, VariableByteArrayInstance> variablesToConvert
	) {
		if (StringUtil.isEmpty(type) || VariableInstanceType.NULL.getTypeKeys().contains(type)) {
			return null;
		}

		VariableInstanceInfo variable = null;
		if (value == null) {
			variable = new VariableDefaultInstance(name, type);

		} else if (value instanceof String || VariableInstanceType.STRING.getTypeKeys().contains(type) || ProcessDefinitionW.VARIABLE_MANAGER_ROLE_NAME.equals(name)) {
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
			VariableByteArrayInstance byteVariable = new VariableByteArrayInstance(name, value);
			variable = byteVariable;

			if (value instanceof Number) {
				variablesToConvert.put((Number) value, byteVariable);
			}
		} else {
			// Try to execute custom methods
			java.sql.Date date = getValueFromCustomMethod(value, "dateValue");
			if (date != null) {
				variable = new VariableDateInstance(name, new Timestamp(date.getTime()));
			}
		}

		@SuppressWarnings("unchecked")
		V result = (V) variable;
		return result;
	}

	private void convertByteValues(Map<Number, VariableByteArrayInstance> variables) {
		if (MapUtil.isEmpty(variables)) {
			return;
		}

		List<Number> ids = new ArrayList<>(variables.keySet());
		String query = "select b.PROCESSFILE_, b.BYTES_ from JBPM_BYTEBLOCK b where ";
		List<Serializable[]> data = getBytesByIds(null, query, 2, ids);
		if (ListUtil.isEmpty(data)) {
			return;
		}

		Map<Number, List<byte[]>> bytesForVariables = new HashMap<>();
		for (Serializable[] info: data) {
			if (ArrayUtil.isEmpty(info) || info.length == 1) {
				continue;
			}

			Number id = null;
			Serializable item = info[0];
			if (item instanceof Number) {
				id = (Number) item;
			} else {
				continue;
			}

			List<byte[]> existingBytes = bytesForVariables.get(id);
			if (existingBytes == null) {
				existingBytes = new ArrayList<>();
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
			if (var == null) {
				continue;
			}

			List<byte[]> values = bytesForVariables.get(id);
			if (ListUtil.isEmpty(values)) {
				continue;
			}

			byte[] bytes = null;
			if (values.size() > 1) {
				int pos = 0;
				bytes = new byte[values.size() * 1024];
				for (Iterator<byte[]> it = values.iterator(); it.hasNext();) {
					byte[] tmp = it.next();
					System.arraycopy(tmp, 0, bytes, pos, tmp.length);
					pos += tmp.length;
				}
			} else {
				bytes = values.get(0);
			}

			var.setValue(bytes);
		}
	}

	private List<Serializable[]> getBytesByIds(List<Serializable[]> data, String query, int columns, List<Number> ids) {
		if (data == null) {
			data = new ArrayList<>();
		}
		if (ListUtil.isEmpty(ids)) {
			return data;
		}

		List<Number> usedIds = null;
		if (ids.size() > 1000) {
			usedIds = new ArrayList<>(ids.subList(0, 1000));
			ids = new ArrayList<>(ids.subList(1000, ids.size()));
		} else {
			usedIds = new ArrayList<>(ids);
			ids = null;
		}

		List<Serializable[]> queriedData = getDataByQuery(query + getQueryParameters("b.PROCESSFILE_", usedIds, false), 2);

		if (queriedData != null) {
			data.addAll(queriedData);
		}

		return getBytesByIds(data, query, columns, ids);
	}

	@SuppressWarnings("unchecked")
	private <T> T getValueFromCustomMethod(Object target, String method) {
		try {
			if (target == null) {
				return null;
			}

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
		 {
			return "substr(".concat(column).concat(", 1, " + getMaxStringValueLength("1048576") + ")");	//	1 MB
		}
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
			boolean isString
	) {
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

	private String getMaxStringValueLength() {
		return getMaxStringValueLength("255");
	}
	private String getMaxStringValueLength(String defaultLength) {
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("max_string_var_length", defaultLength);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByNames(List<String> names) {
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

			String maxLength = getMaxStringValueLength();
			if (!StringUtil.isEmpty(maxLength)) {
				query = StringHandler.replace(query, "var.stringvalue_ as sv", "substr(var.stringvalue_, 1, " + maxLength + ") as sv");
			}

			data = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: '" + query + "'. Error getting variables by names: " + names, e);
		}

		return getConverted(data);
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
	public <V extends VariableInstance> Collection<V> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds) {
		return getFullVariablesByProcessInstanceIdsNaiveWay(processInstanceIds, null);
	}

	@Override
	public <V extends VariableInstance> Collection<V> getFullVariablesByProcessInstanceIdsNaiveWay(List<Long> processInstanceIds, List<Long> existingVars) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return null;
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(existingVars, processInstanceIds);
		}

		int columns = COLUMNS + 3;
		List<Serializable[]> data = null;
		String query = getQuery("select ", STANDARD_COLUMNS, ", ", getSubstring("var.STRINGVALUE_"), " as sv, var.taskinstance_, var.PROCESSINSTANCE_ ",
				FROM, " where ",
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

		return getConverted(data);
	}

	@Override
	public void onApplicationEvent(final ApplicationEvent event) {
		if (event instanceof VariableCreatedEvent) {
			VariableCreatedEvent variableEvent = (VariableCreatedEvent) event;

			Map<String, Object> createdVariables = variableEvent.getVariables();
			Serializable procInstId = variableEvent.getProcessInstanceId();
			if (procInstId instanceof Long) {
				Long processInstanceId = (Long) procInstId;
				bindProcessVariables(variableEvent.getProcessDefinitionName(), processInstanceId, createdVariables == null ? null : createdVariables.keySet());

				doIndexVariables(processInstanceId);
			}
		} else if (event instanceof TaskInstanceSubmitted) {
			TaskInstanceSubmitted taskSubmittedEvent = (TaskInstanceSubmitted) event;
			Serializable piId = taskSubmittedEvent.getProcessInstanceId();
			if (piId instanceof Number) {
				doIndexVariables(((Number) piId).longValue());
			} else if (StringHandler.isNumeric(piId.toString())) {
				doIndexVariables(Integer.valueOf(piId.toString()).longValue());
			}
		} else if (event instanceof ProcessInstanceCreatedEvent) {
			Serializable piId = ((ProcessInstanceCreatedEvent) event).getProcessInstanceId();
			if (piId != null && StringHandler.isNumeric(piId.toString())) {
				doIndexVariables(Long.valueOf(piId.toString()));
			}
		}
	}

	@Override
	@Transactional
	public void doIndexVariables(Long piId) {
		try {
			if (!IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm_index_vars_dynamically", Boolean.TRUE)) {
				return;
			}

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
					try {
						FullTextSession indexer = org.hibernate.search.Search.getFullTextSession(context.getSession());
						Transaction tx = indexer.beginTransaction();
						for (Variable variable: variables) {
							if (variable.getValue() == null) {
								continue;
							}

							Object var = indexer.load(Variable.class, variable.getId());
							indexer.index(var);
						}
						tx.commit();

						return true;
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error indexing new variables for proc. inst. by ID: " + piId + ", variables: " + variables, e);
					}
					return false;
				}
			});
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error indexing new variables for proc. inst. by ID: " + piId, e);
		}
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
		bindProcessVariables(processDefinitionName, getAllProcDefVariableBinds(), getVariablesByProcessInstanceId(processInstanceId), createdVariables);
	}

	@Transactional(readOnly = false)
	private void bindProcessVariables(
			String processDefinitionName,
			List<ProcessDefinitionVariablesBind> currentBinds,
			Collection<VariableInstanceInfo> currentVariables,
			Set<String> createdVariables
	) {
		if (StringUtil.isEmpty(processDefinitionName)) {
			return;
		}

		currentBinds = currentBinds == null ? new ArrayList<>() : currentBinds;

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

			if (currentBinds == null) {
				currentBinds = new ArrayList<>();
			}

			currentBinds.add(bind);
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
	public <V extends VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesByProcesses(
			Map<String, List<Serializable>> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables
	) {
		if (MapUtil.isEmpty(activeVariables)) {
			LOGGER.warning("There are no criterias provided for the variables, terminating data mining");
			return null;
		}

		Map<String, VariableQuerierData> queryParams = getConvertedData(activeVariables);
		return getVariablesByNamesAndValuesAndExpressionsByProcesses(queryParams, variables, procDefNames, procInstIds, flexibleVariables);
	}

	private Map<String, VariableQuerierData> getConvertedData(Map<String, List<Serializable>> activeVariables){
		if (MapUtil.isEmpty(activeVariables)) {
			return null;
		}

		Map<String, VariableQuerierData> convertedVariables = new HashMap<>();

		for (String key: activeVariables.keySet()) {
			convertedVariables.put(key, new VariableQuerierData(key, 0, true, activeVariables.get(key)));
		}

		return convertedVariables;
	}

	@Override
	public <V extends VariableInstance> Collection<V> getVariablesByProcessInstanceIds(Collection<Long> procInstIds) {
		if (ListUtil.isEmpty(procInstIds)) {
			return null;
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(null, null, new ArrayList<>(procInstIds));
		}

		String query = getQuery(getSelectPart(STANDARD_COLUMNS), getFromClause(false, false), " where ", PROC_INST_IDS_EXPRESSION, " group by var.name_");
		return getVariablesByInstanceIds(query, COLUMNS, new ArrayList<>(procInstIds), PROC_INST_IDS_EXPRESSION);
	}

	@Override
	public <V extends VariableInstance> List<V> getVariablesByNameAndTaskInstance(final Collection<String> names, final Long tiId) {
		if (ListUtil.isEmpty(names) || tiId == null) {
			return null;
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesCollectionByLucene(null, new ArrayList<>(names), null, null, Arrays.asList(tiId), null, null, false);
		}

		String query = getQuery(getSelectPart(STANDARD_COLUMNS), CoreConstants.COMMA.concat(" var.STRINGVALUE_ as sv").concat(CoreConstants.COMMA), OTHER_VALUES, getFromClause(true, false),
				" where var.taskinstance_ = ",
				String.valueOf(tiId),
				" and ",
				getQueryParameters("var.name_", names, Boolean.TRUE)
		);
		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, FULL_COLUMNS);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(results)) {
			return null;
		}

		Collection<V> vars = getConverted(results);
		if (ListUtil.isEmpty(vars)) {
			return null;
		}
		return new ArrayList<>(vars);
	}

	@Override
	public <V extends VariableInstance> Map<Long, List<V>> getGroupedVariables(Collection<V> variables) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}

		Map<Long, List<V>> grouped = new HashMap<>();
		for (V var: variables) {
			Serializable piId = var.getProcessInstanceId();
			if (!(piId instanceof Long)) {
				continue;
			}

			Long procInstId = (Long) piId;
			List<V> vars = grouped.get(procInstId);
			if (vars == null) {
				vars = new ArrayList<>();
				grouped.put(procInstId, vars);
			}
			vars.add(var);
		}

		return grouped;
	}

	@Override
	public <V extends VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables
		) {

		return getVariablesByNamesAndValuesAndExpressionsByProcesses(
				activeVariables,
				variables,
				procDefNames,
				procInstIds,
				flexibleVariables,
				false
		);
	}

	private EntityManager entityManager;

	@Override
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public EntityManager getEntityManager() {
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

			if (sentences != null && sentences.size() > 1) {
				multipleValues = true;
			}
		}

		@Override
		public String toString() {
			return "Field: " + field + ", value to match: " + value + ", flexible: " + flexible + ", multiple values: " + multipleValues +
					", sentences: " + sentences;
		}
	}

	private boolean isLikeExpressionEnabled() {
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.search_vars_full_like", Boolean.FALSE);
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

				flexible = MapUtil.isEmpty(flexibleVariables) || !flexibleVariables.containsKey(name) ? false : flexibleVariables.get(name);
				if (flexible) {
					if (isLikeExpressionEnabled()) {
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
				 valueToMatch.toString().indexOf(CoreConstants.MINUS) != -1 ||
				(previousValueToMatch != null && !ListUtil.isEmpty(previousValueToMatch.sentences)))) {
			sentence = valueToMatch.toString();

			sentences = previousValueToMatch == null ? null : previousValueToMatch.sentences;
			if (sentences == null) {
				sentences = new ArrayList<>();
			}
			sentences.add(sentence);
		}
		boolean multipleValues = false;
		if (previousValueToMatch != null && previousValueToMatch.value != null) {
			String currentValue = String.valueOf(valueToMatch);

			sentences = previousValueToMatch == null ? null : previousValueToMatch.sentences;
			if (sentences != null) {
				sentences.add(currentValue);
			}

			String previousValue = String.valueOf(previousValueToMatch.value);
			valueToMatch = previousValue.concat(CoreConstants.SPACE).concat(currentValue);
			multipleValues = true;
		}
		return new ValueToMatch(field, valueToMatch, flexible, multipleValues, sentences);
	}

	private BooleanJunction<?> getVariablesConditions(
			Class<?> queryType,
			QueryBuilder query,
			VariableQuerierData activeVariable,
			List<String> names,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			Map<String, Boolean> flexibleVariables
	) {
		BooleanJunction<?> conditions = query.bool();

		if (VariableBytes.class.getName().equals(queryType.getName())) {
			conditions.must(
					query.phrase()
						.onField("bytes")
						.sentence(activeVariable.getValues().toString())
						.createQuery()
			);
			return conditions;
		}

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
					if (valueToMatch.toString().indexOf(CoreConstants.MINUS) == -1) {
						matching = query.keyword()
											.wildcard()						//	To support LIKE
										.onField(valueToMatch.field)
										.matching(valueToMatch.value);
					} else {
						matching = query.keyword()
										.onField(valueToMatch.field)
										.matching(valueToMatch.value);
					}
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

				if (matching != null) {
					conditions.must(matching.createQuery());
				}
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

	private Class<?> getQueryType(VariableQuerierData queryData) {
		if (queryData == null) {
			return Variable.class;
		}

		String varName = queryData.getName();
		if (varName.startsWith(VariableInstanceType.LIST.getPrefix()) || varName.startsWith(VariableInstanceType.OBJ_LIST.getPrefix())) {
			return VariableBytes.class;
		}

		return Variable.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> List<T> getResults(
			Class<?> queryType,
			Class<T> resultsType,

			FullTextSession fullTextSession,
			Transaction tx,
			BooleanJunction<?> conditions,
			String... fields
	) {
		try {
			FullTextQuery query = fullTextSession.createFullTextQuery(conditions.createQuery());
			if (queryType.getName().equals(VariableBytes.class.getName())) {
				//	1. Load variable bytes by conditions
				List<VariableBytes> varsBytes = query.list();
				if (ListUtil.isEmpty(varsBytes)) {
					return null;
				}

				//	2. Find variables by bytes
				Map<Long, Boolean> ids = new HashMap<>();
				for (VariableBytes varBytes: varsBytes) {
					ids.put(varBytes.getProcessFile(), Boolean.TRUE);
				}
				tx.commit();
				List<Variable> vars = bpmDAO.getVariablesByBytes(new ArrayList<>(ids.keySet()));
				if (ListUtil.isEmpty(vars)) {
					return null;
				}

				if (!Variable.class.getName().equals(resultsType.getName())) {
					Map<Long, Boolean> procInstIds = new HashMap<>();
					for (Variable var: vars) {
						procInstIds.put(var.getProcessInstance(), Boolean.TRUE);
					}
					return new ArrayList(procInstIds.values());
				}

				return (List<T>) vars;
			} else {
				if (!ArrayUtil.isEmpty(fields)) {
					query.setProjection(fields);
				}
				return query.list();
			}
		} finally {
			if (tx != null && !tx.wasCommitted()) {
				tx.commit();
			}
		}
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

					Class<?> queryType = getQueryType(queryData);

					QueryBuilder queryBuilder = fullTextSession
													.getSearchFactory()
													.buildQueryBuilder()
													.forEntity(queryType)
													.get();

					BooleanJunction<?> conditions = getVariablesConditions(
														queryType,
														queryBuilder,
														queryData,
														queryData == null ? names : null,
														procInstIds,
														taskInstIds,
														varIds,
														flexibleVariables
													);
					return getResults(queryType, Variable.class, fullTextSession, tx, conditions);
				}
			});
			return data;
		} finally {
			if (CoreUtil.isSQLMeasurementOn()) {
				CoreUtil.doDebugSQL(start, System.currentTimeMillis(), (lucene? "LUCENE" : "HIBERNATE") + ": query for variable name(s): " +
						names + ", proc. inst. IDs: " + procInstIds + ", task inst. IDs: " + taskInstIds + " and var. inst. IDs: " + varIds + ". Query: " + queryData);
			}
		}
	}

	private List<Long> getProcessInstanceIdsFromVariablesByLucene(
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
			return getBpmContext().execute(new JbpmCallback<List<Long>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<Long> doInJbpm(JbpmContext context) throws JbpmException {
					Session session = context.getSession();
					FullTextSession fullTextSession = Search.getFullTextSession(session);
					Transaction tx = fullTextSession.beginTransaction();

					Class<?> queryType = getQueryType(queryData);
					QueryBuilder queryBuilder = fullTextSession
													.getSearchFactory()
													.buildQueryBuilder()
													.forEntity(queryType)
													.get();

					BooleanJunction<?> conditions = getVariablesConditions(
														queryType,
														queryBuilder,
														queryData,
														queryData == null ? names : null,
														procInstIds,
														taskInstIds,
														varIds,
														flexibleVariables
													);

					List<?> results = getResults(queryType, Long[].class, fullTextSession, tx, conditions, "processInstance");
					if (ListUtil.isEmpty(results)) {
						return null;
					}

					if (queryType.getName().equals(VariableBytes.class.getName())) {
						return (List<Long>) results;
					} else {
						List<Long[]> procInstIds = (List<Long[]>) results;

						Map<Long, Boolean> allProcInstIds = new HashMap<>();
						for (Object[] ids: procInstIds) {
							if (ArrayUtil.isEmpty(ids)) {
								continue;
							}

							for (Object id: ids) {
								if (id instanceof Number) {
									allProcInstIds.put(((Number) id).longValue(), Boolean.TRUE);
								}
							}
						}

						return new ArrayList<>(allProcInstIds.keySet());
					}
				}
			});
		} finally {
			if (CoreUtil.isSQLMeasurementOn()) {
				CoreUtil.doDebugSQL(start, System.currentTimeMillis(), (lucene? "LUCENE" : "HIBERNATE") + ": query for proc. instance IDs by variable name(s): " + names + ", proc. inst. IDs: " +
						procInstIds + ", task inst. IDs: " + taskInstIds + " and var. inst. IDs: " + varIds + ". Query: " + queryData);
			}
		}
	}

	private <V extends VariableInstance> List<V> getVariablesCollectionByLucene(List<Long> varIds, List<Long> procInstIds) {
		return getVariablesCollectionByLucene(null, null, null, procInstIds, null, varIds, null, false);
	}

	private <V extends VariableInstance> List<V> getVariablesCollectionByLucene(List<String> variables, List<String> procDefNames, List<Long> procInstIds) {
		return getVariablesCollectionByLucene(null, variables, procDefNames, procInstIds, null, null, null, false);
	}

	private <V extends VariableInstance> List<V> getVariablesCollectionByLucene(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables
	) {
		Map<Long, Map<String, V>> vars = getVariablesByLucene(
				activeVariables,
				variables,
				procDefNames,
				procInstIds,
				taskInstIds,
				varIds,
				flexibleVariables,
				strictBinaryVariables,
				false
		);

		List<V> results = getConverted(vars);

		return results;
	}

	@Override
	public <V extends VariableInstance> List<V> getConverted(Map<Long, Map<String, V>> vars) {
		if (MapUtil.isEmpty(vars)) {
			return null;
		}

		List<V> results = new ArrayList<>();
		for (Map<String, V> procVars: vars.values()) {
			results.addAll(procVars.values());
		}
		return results;
	}

	private <V extends VariableInstance> Map<Long, Map<String, V>> getVariablesByLucene(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables,
			boolean selectOnlyProcInstIds
	) {
		return getVariablesByLucene(
				activeVariables,
				variables,
				procDefNames,
				procInstIds,
				null,
				null,
				flexibleVariables,
				strictBinaryVariables,
				selectOnlyProcInstIds
		);
	}

	private <V extends VariableInstance> Map<Long, Map<String, V>> getConverted(Collection<V> vars) {
		if (ListUtil.isEmpty(vars)) {
			return null;
		}

		Map<Long, Map<String, V>> results = new HashMap<>();
		for (V var: vars) {
			Serializable value = var.getVariableValue();
			if (value == null) {
				continue;
			}
			String name = var.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}

			Serializable piId = var.getProcessInstanceId();
			Long procInstId = piId instanceof Long ? (Long) piId : null;
			if (procInstId == null) {
				continue;
			}

			Map<String, V> procInstData = results.get(procInstId);
			if (procInstData == null) {
				procInstData = new HashMap<>();
				results.put(procInstId, procInstData);
			}

			procInstData.put(name, var);
		}
		return results;
	}

	private <V extends VariableInstance> Map<Long, Map<String, V>> getVariablesByLucene(
			Map<String, VariableQuerierData> keywords,
			List<String> variables,
			List<String> procDefNames,
			List<Long> procInstIds,
			List<Long> taskInstIds,
			List<Long> varIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables,
			boolean selectOnlyProcInstIds
	) {
		try {
			if (variables != null) {
				variables = new ArrayList<>(variables);
			}

			Map<Long, Map<String, V>> results = null;

			if (MapUtil.isEmpty(keywords)) {
				if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.load_vars_by_hib_no_kw", Boolean.FALSE)) {
					List<Variable> vars = getVariablesByLucene(null, variables, procInstIds, taskInstIds, varIds, flexibleVariables);
					if (ListUtil.isEmpty(vars)) {
						getLogger().warning("Nothing found from Hibernate by var. names: " + variables + ", proc. inst. IDs: " + procInstIds +
								", task inst. IDs: " + taskInstIds + ", var. inst. IDs: " + varIds);
						return null;
					}
					results = getConvertedVariables(vars);
				} else {
					List<Serializable[]> rawData = getInformationByVariablesNameAndValuesAndProcesses(
							null,
							variables,
							procDefNames,
							procInstIds,
							true,
							false,
							strictBinaryVariables
					);
					if (ListUtil.isEmpty(rawData)) {
						getLogger().warning("Nothing found from SQL by var. names: " + variables + ", proc. inst. IDs: " + procInstIds +
								", task inst. IDs: " + taskInstIds + ", var. inst. IDs: " + varIds);
						return null;
					}
					results = getGroupedData(getConverted(rawData));
				}
			} else {
				if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("bpm.load_var_by_sql_ids", Boolean.TRUE) &&
						!ListUtil.isEmpty(procInstIds) || !ListUtil.isEmpty(taskInstIds) || !ListUtil.isEmpty(varIds)) {
					List<Serializable[]> data = getInformationByVariablesNameAndValuesAndProcesses(
							null,
							null,
							null,
							new ArrayList<>(keywords.values()),
							variables,
							null,
							procInstIds,
							taskInstIds,
							varIds,
							true,
							false,
							strictBinaryVariables
					);
					Collection<V> vars = getConverted(data);
					results = getConverted(vars);
					if (MapUtil.isEmpty(results)) {
						getLogger().warning("Nothing found from database by keywords " + keywords + " var. names: " + variables +
								", proc. inst. IDs: " + procInstIds + ", task inst. IDs: " + taskInstIds + ", var. inst. IDs: " + varIds);
					}
					return results;
				}

				//	1. Find proc. inst. IDs from variables by conditions
				List<Variable> vars = null;
				boolean loadAddtionalVars = !ListUtil.isEmpty(variables);
				Map<Long, Boolean> allFoundedProcInstIds = null;
				Map<Long, List<Variable>> allVariables = null;
				for (String varName: keywords.keySet()) {
					VariableQuerierData queryData = keywords.get(varName);
					String searchExpression = null;
					boolean checkValues = false;
					if (queryData.getValues() != null && queryData.getValues().size() == 1) {
						searchExpression = queryData.getValues().get(0).toString();
						checkValues = searchExpression.contains(CoreConstants.MINUS);
						searchExpression = searchExpression.toLowerCase();
					}

					List<?> tmpResults = selectOnlyProcInstIds ?
							getProcessInstanceIdsFromVariablesByLucene(queryData, variables, procInstIds, taskInstIds, varIds, flexibleVariables) :
							getVariablesByLucene(queryData, variables, procInstIds, taskInstIds, varIds, flexibleVariables);
					if (ListUtil.isEmpty(tmpResults)) {
						getLogger().warning("Nothing found from Lucene by " + queryData + ". Terminating search");
						return null;
					}

					Map<Long, Boolean> foundedProcInstIds = new HashMap<>();
					for (Object tmp: tmpResults) {
						Long piId = null;
						Variable var = null;

						boolean varObject = true;
						if (tmp instanceof Number) {
							piId = ((Number) tmp).longValue();
							varObject = false;
						} else if (tmp instanceof Variable) {
							var = (Variable) tmp;

							if (checkValues && !var.getValue().toString().toLowerCase().contains(searchExpression.toLowerCase())) {
								continue;
							}

							piId = var.getProcessInstance();
						}

						foundedProcInstIds.put(piId, Boolean.TRUE);

						if (loadAddtionalVars) {
							if (variables == null) {
								variables = new ArrayList<>();
							}
							if (!variables.contains(queryData.getName())) {
								variables.add(queryData.getName());
							}
						} else {
							if (allVariables == null) {
								allVariables = new HashMap<>();
							}

							if (varObject) {
								List<Variable> varsByProcInst = allVariables.get(piId);
								if (varsByProcInst == null) {
									varsByProcInst = new ArrayList<>();
									allVariables.put(piId, varsByProcInst);
								}
								varsByProcInst.add(var);
							}
						}
					}

					boolean firstHit = allFoundedProcInstIds == null;
					if (firstHit) {
						allFoundedProcInstIds = new HashMap<>(foundedProcInstIds);
					} else {
						allFoundedProcInstIds.keySet().retainAll(foundedProcInstIds.keySet());
					}
					if (ListUtil.isEmpty(procInstIds)) {
						procInstIds = new ArrayList<>(allFoundedProcInstIds.keySet());
					} else {
						procInstIds.retainAll(allFoundedProcInstIds.keySet());
					}
					if (ListUtil.isEmpty(procInstIds)) {
						return null;
					}
				}

				//	2. Making sure only requested data will be returned
				if (MapUtil.isEmpty(allFoundedProcInstIds)) {
					return null;
				}

				if (!ListUtil.isEmpty(procInstIds)) {
					allFoundedProcInstIds.keySet().retainAll(procInstIds);
				}

				if (MapUtil.isEmpty(allFoundedProcInstIds)) {
					return null;
				}

				//	3. Find rest of variables by eligible proc. inst. IDs
				if (loadAddtionalVars) {
					vars = getVariablesByLucene(null, variables, new ArrayList<>(allFoundedProcInstIds.keySet()), taskInstIds, varIds, null);
				} else {
					allVariables.keySet().retainAll(allFoundedProcInstIds.keySet());
					vars = new ArrayList<>();
					for (Collection<Variable> procInstVars: allVariables.values()) {
						vars.addAll(procInstVars);
					}
				}

				if (!selectOnlyProcInstIds) {
					if (ListUtil.isEmpty(vars)) {
						return null;
					}

					results = getConvertedVariables(vars);
					if (MapUtil.isEmpty(results)) {
						getLogger().warning("Unable to convert " + vars);
						return null;
					}
				}
			}

			if (!ListUtil.isEmpty(procInstIds) && (!selectOnlyProcInstIds && !MapUtil.isEmpty(results))) {
				//	Making sure results are correct
				results.keySet().retainAll(procInstIds);
			}

			if (selectOnlyProcInstIds) {
				results = new HashMap<>();
				for (Long piId: procInstIds) {
					results.put(piId, null);
				}
			}
			return results;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error querying for " + variables + " by " + keywords + " and proc. inst. IDs: " + procInstIds, e);
		}

		return null;
	}

	@Override
	public <V extends VariableInstance> Map<Long, Map<String, V>> getConvertedVariables(List<Variable> variables) {
		if (ListUtil.isEmpty(variables)) {
			return Collections.emptyMap();
		}

		Map<Number, VariableByteArrayInstance> variablesToConvert = new HashMap<>();
		Map<Long, Map<String, V>> converted = new HashMap<>();
		for (Variable variable: variables) {
			Long piId = variable.getProcessInstance();
			if (piId == null) {
				continue;
			}

			Map<String, V> procInstData = converted.get(piId);
			if (procInstData == null) {
				procInstData = new HashMap<>();
				converted.put(piId, procInstData);
			}

			String name = variable.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}

			Character type = variable.getClassType();
			Serializable value = variable.getValue();
			if (value == null) {
				continue;
			}

			V var = getVariable(
					variable.getId(),
					name,
					value,
					type == null ? null : String.valueOf(type),
					variablesToConvert
			);
			V existingVar = procInstData.get(name);
			if (existingVar == null) {
				existingVar = var;
			}

			if (existingVar == null) {
				continue;
			}

			if (variable.getTaskInstance() != null && existingVar instanceof VariableInstanceInfo) {
				VariableInstanceInfo filledVar = (VariableInstanceInfo) existingVar;

				if (filledVar.getId() == null) {
					filledVar.setId(variable.getId());
				}

				if (variable.getId() != null && variable.getId() >= filledVar.getId()) {
					filledVar.setId(variable.getId());
					filledVar.setValue(value);
				}
				filledVar.setProcessInstanceId(variable.getProcessInstance());
				filledVar.setTaskInstanceId(variable.getTaskInstance());

				@SuppressWarnings("unchecked")
				V temp = (V) filledVar;
				existingVar = temp;
			}
			procInstData.put(name, existingVar);
		}

		convertByteValues(variablesToConvert);
		if (!MapUtil.isEmpty(variablesToConvert)) {
			getLogger().info("Convert: " + variablesToConvert);
		}

		return converted;
	}

	private boolean isLuceneQueryingTurnedOn() {
		return IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("use_lucene_for_bpm_search", Boolean.TRUE);
	}

	@Override
	public <V extends VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> originalProcInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables
	) {
		return getVariablesByNamesAndValuesAndExpressionsByProcesses(
				activeVariables,
				variables,
				procDefNames,
				originalProcInstIds,
				flexibleVariables,
				strictBinaryVariables,
				false
		);
	}

	@Override
	public <V extends VariableInstance> Map<Long, Map<String, V>> getVariablesByNamesAndValuesAndExpressionsByProcesses(
			Map<String, VariableQuerierData> activeVariables,
			List<String> variables,
			List<String> procDefNames,
			List<Long> originalProcInstIds,
			Map<String, Boolean> flexibleVariables,
			boolean strictBinaryVariables,
			boolean selectOnlyProcInstIds
	) {

		if (!MapUtil.isEmpty(activeVariables)) {
			List<VariableQuerierData> queryData = new ArrayList<>(activeVariables.values());
			Collections.sort(queryData, new Comparator<VariableQuerierData>() {

				@Override
				public int compare(VariableQuerierData var1, VariableQuerierData var2) {
					return -1 * (Integer.valueOf(var1.getOrder()).compareTo(var2.getOrder()));
				}
			});

			activeVariables = new LinkedHashMap<>();
			for (VariableQuerierData activeVar: queryData) {
				if (activeVar != null) {
					activeVariables.put(activeVar.getName(), activeVar);
				}
			}
		}

		if (isLuceneQueryingTurnedOn()) {
			return getVariablesByLucene(
					activeVariables,
					variables,
					procDefNames,
					originalProcInstIds,
					flexibleVariables,
					strictBinaryVariables,
					selectOnlyProcInstIds
			);
		}

		List<String> variablesToQuery = new ArrayList<>();
		List<Long> procInstIds = originalProcInstIds == null ? null : new ArrayList<>(originalProcInstIds);

		if (!MapUtil.isEmpty(activeVariables)) {
			variablesToQuery.addAll(activeVariables.keySet());
		}

		Map<Long, Map<String, V>> results = new LinkedHashMap<>();

		if (!MapUtil.isEmpty(activeVariables)) {
			for (String variableName: activeVariables.keySet()) {
				if (variablesToQuery.contains(variableName)) {
					continue;
				}

				variablesToQuery.add(variableName);
			}
		}

		Map<String, VariableQuerierData> variablesAndValuesToQuery = new HashMap<>();
		for (String variableToQuery: variablesToQuery) {
			variablesAndValuesToQuery.put(variableToQuery, activeVariables.get(variableToQuery));
		}

		//	Querying the DB for variables
		Collection<V> info = getProcessVariablesByNamesAndValues(
				variablesAndValuesToQuery,
				variables,
				ListUtil.isEmpty(procInstIds) ? procDefNames : null, procInstIds,
				true,
				flexibleVariables,
				strictBinaryVariables
		);

		if (ListUtil.isEmpty(info)) {
			return null;
		}

		//	Making copy of cached process instance IDs
		List<Long> cachedProcInstIds = MapUtil.isEmpty(results) ? null : new ArrayList<>(results.keySet());
		//	Marking process instance IDs of queried data
		Map<Long, Boolean> queriedIds = new HashMap<>();

		//	Combining cached results and queried results
		for (V varInfo: info) {
			if (varInfo == null) {
				continue;
			}
			Serializable procInstId = varInfo.getProcessInstanceId();
			Long piId = procInstId instanceof Long ? (Long) procInstId : null;
			if (piId == null || !ListUtil.isEmpty(originalProcInstIds) && !originalProcInstIds.contains(piId)) {
				continue;
			}

			queriedIds.put(piId, Boolean.TRUE);

			//	Getting variables of a process
			Map<String, V> processVariables = results.get(piId);
			if (processVariables == null) {
				processVariables = new HashMap<>();
				results.put(piId, processVariables);
			}

			if (varInfo.getVariableValue() != null) {
				V existingValue = processVariables.get(varInfo.getName());
				if (existingValue == null) {
					processVariables.put(varInfo.getName(), varInfo);
				} else {
					Long varId = varInfo.getVariableId();
					Long existingId = existingValue.getVariableId();
					if (varId != null && existingId != null && varId > existingId) {
						processVariables.put(varInfo.getName(), varInfo);
					}
				}
			}
		}

		if (!MapUtil.isEmpty(variablesAndValuesToQuery) && !MapUtil.isEmpty(queriedIds) && !ListUtil.isEmpty(cachedProcInstIds)) {
			List<Long> idsToRemove = new ArrayList<>();

			//	Checking if anything was found by criteria(s) in DB that was found in cache by other criteria(s)
			for (Long piId: cachedProcInstIds) {
				if (!queriedIds.containsKey(piId)) {
					idsToRemove.add(piId);
				}
			}

			if (!ListUtil.isEmpty(idsToRemove)) {
				results.keySet().removeAll(idsToRemove);
			}
		} else {
			if (!ListUtil.isEmpty(originalProcInstIds)) {
				results.keySet().retainAll(originalProcInstIds);
			}
		}

		return results;
	}

	@Override
	public <V extends VariableInstance> Map<Long, Map<String, V>> getGroupedData(Collection<V> variables) {
		return getGroupedData(variables, false);
	}

	@Override
	public <V extends VariableInstance> Map<Long, Map<String, V>> getGroupedData(Collection<V> variables, boolean byTaskInstance) {
		if (ListUtil.isEmpty(variables)) {
			return null;
		}

		Map<Long, Map<String, V>> data = new LinkedHashMap<>();
		for (V var: variables) {
			Serializable instId = byTaskInstance ? var.getTaskInstanceId() : var.getProcessInstanceId();
			Long id = instId instanceof Long ? (Long) instId : null;
			if (id == null) {
				continue;
			}

			String name = var.getName();
			if (StringUtil.isEmpty(name)) {
				continue;
			}

			Serializable value = var.getVariableValue();
			if (value == null) {
				continue;
			}

			Map<String, V> groupedData = data.get(id);
			if (groupedData == null) {
				groupedData = new HashMap<>();
				data.put(id, groupedData);
			}

			if (groupedData.containsKey(name)) {
				continue;
			}

			groupedData.put(name, var);
		}
		return data;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.VariableInstanceQuerier#findJBPMVariableValues(java.lang.String, java.lang.String, java.lang.String, java.lang.Long)
	 */
	@Override
	public List<Serializable> findJBPMVariableValues(
			String requiredVariableName,
			String argumentVariableName,
			String argumentValue,
			Long processInstance) {

		List<VariableInstanceInfo> jbpmVariables = findJBPMVariable(
				requiredVariableName,
				argumentVariableName,
				argumentValue,
				processInstance);
		if (ListUtil.isEmpty(jbpmVariables)) {
			return Collections.emptyList();
		}

		List<Serializable> values = new ArrayList<>(jbpmVariables.size());
		for (VariableInstanceInfo jbpmVariable: jbpmVariables) {
			Serializable value = jbpmVariable.getValue();
			if (value == null) {
				continue;
			}

			values.add(value);
		}

		return values;
	}
	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.VariableInstanceQuerier#findJBPMVariable(java.lang.String, java.lang.String, java.lang.String, java.lang.Long)
	 */
	@Override
	public <V extends VariableInstance> List<V> findJBPMVariable(
			String requiredVariableName,
			String argumentVariableName,
			String argumentValue,
			Long processInstanceId
	) {
		if (StringUtil.isEmpty(requiredVariableName)) {
			return Collections.emptyList();
		}

		/* Will search for: */
		List<String> variables = new ArrayList<>(1);
		variables.add(requiredVariableName);

		/* by: */
		Map<String, VariableQuerierData> variablesWithValues = null;
		if (!StringUtil.isEmpty(argumentVariableName) && !StringUtil.isEmpty(argumentValue)) {
			ArrayList<Serializable> serializableList = new ArrayList<>(1);
			serializableList.add(argumentValue);

			VariableQuerierData variableQuerierData = new VariableQuerierData(
					argumentVariableName,
					0,
					false,
					serializableList
			);

			variablesWithValues = new HashMap<>(1);
			variablesWithValues.put(argumentVariableName, variableQuerierData);
		}

		ArrayList<Long> processInstanceIds = null;
		if (processInstanceId != null) {
			processInstanceIds = new ArrayList<>(1);
			processInstanceIds.add(processInstanceId);
		}

		/* Let's query: */
		Map<Long, Map<String, V>> resultsMapByProcessInstance = getVariablesByNamesAndValuesAndExpressionsByProcesses(
				variablesWithValues,
				variables,
				null,
				processInstanceIds,
				null
		);
		if (MapUtil.isEmpty(resultsMapByProcessInstance)) {
			return Collections.emptyList();
		}

		/* Parsing results */
		List<V> variableInstances = new ArrayList<>();
		Map<String, V> resultsMapByVariableName = null;
		for (Long localProcessInstanceId: resultsMapByProcessInstance.keySet()) {
			resultsMapByVariableName = resultsMapByProcessInstance.get(localProcessInstanceId);
			if (MapUtil.isEmpty(resultsMapByVariableName)) {
				continue;
			}

			V requiredVariable = resultsMapByVariableName.get(requiredVariableName);
			if (requiredVariable == null) {
				continue;
			}

			variableInstances.add(requiredVariable);
		}

		return variableInstances;
	}

}