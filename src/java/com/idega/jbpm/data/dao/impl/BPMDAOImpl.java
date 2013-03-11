package com.idega.jbpm.data.dao.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.def.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.block.process.data.CaseBMPBean;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.DatastoreInterface;
import com.idega.data.OracleDatastoreInterface;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.bean.VariableInstanceType;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.AutoloadedProcessDefinition;
import com.idega.jbpm.data.BPMVariableData;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.ProcessDefinitionVariablesBind;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.events.VariableCreatedEvent;
import com.idega.jbpm.identity.Role;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.28 $ Last modified: $Date: 2009/02/13 17:06:30 $ by $Author: donatas $
 */

@Repository("bpmBindsDAO")
@Transactional(readOnly = true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BPMDAOImpl extends GenericDaoImpl implements BPMDAO, ApplicationListener<VariableCreatedEvent> {

	private static final Logger LOGGER = Logger.getLogger(BPMDAOImpl.class.getName());

	@Autowired
	private BPMContext bpmContext;

	@Autowired(required=false)
	private VariableInstanceQuerier variablesQuerier;

	@Override
	public ViewTaskBind getViewTaskBind(long taskId, String viewType) {

		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
		        .setParameter(ViewTaskBind.taskIdParam, taskId).setParameter(
		            ViewTaskBind.viewTypeParam, viewType).getResultList();

		return binds.isEmpty() ? null : binds.iterator().next();
	}

	@Override
	public ViewTaskBind getViewTaskBindByTaskInstance(long taskInstanceId,
	        String viewType) {

		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager()
		        .createNamedQuery(
		            ViewTaskBind.GET_UNIQUE_BY_TASK_INSTANCE_ID_AND_VIEW_TYPE_QUERY_NAME)
		        .setParameter(ViewTaskBind.taskInstanceIdProp, taskInstanceId)
		        .setParameter(ViewTaskBind.viewTypeParam, viewType)
		        .getResultList();

		return binds.isEmpty() ? null : binds.iterator().next();
	}

	@Override
	public List<ViewTaskBind> getViewTaskBindsByTaskId(long taskId) {

		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.getViewTaskBindsByTaskId).setParameter(
		    ViewTaskBind.taskIdParam, taskId).getResultList();

		return binds;
	}

	@Override
	public List<ViewTaskBind> getViewTaskBindsByTaskInstanceId(
	        long taskInstanceId) {

		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.getViewTaskBindsByTaskInstanceId).setParameter(
		    ViewTaskBind.taskInstanceIdProp, taskInstanceId).getResultList();

		return binds;
	}

	@Override
	public ViewTaskBind getViewTaskBindByView(String viewId, String viewType) {

		return getSingleResult(
		    ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME,
		    ViewTaskBind.class, new Param(ViewTaskBind.viewIdParam, viewId),
		    new Param(ViewTaskBind.viewTypeParam, viewType));
	}

	@Override
	public List<ViewTaskBind> getViewTaskBindsByTasksIds(
	        Collection<Long> taskIds) {

		@SuppressWarnings("unchecked")
		List<ViewTaskBind> viewTaskBinds = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_VIEW_TASK_BINDS_BY_TASKS_IDS).setParameter(
		    ViewTaskBind.tasksIdsParam, taskIds).getResultList();

		return viewTaskBinds;
	}

	@Override
	public Task getTaskFromViewTaskBind(ViewTaskBind viewTaskBind) {

		return (Task) getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_VIEW_TASK).setParameter(
		    ViewTaskBind.viewTypeParam, viewTaskBind.getViewType())
		        .setParameter(ViewTaskBind.taskIdParam,
		            viewTaskBind.getTaskId()).getSingleResult();
	}

	@Override
	public ProcessManagerBind getProcessManagerBind(String processName) {

		ProcessManagerBind pmb = getSingleResult(
		    ProcessManagerBind.getByProcessName, ProcessManagerBind.class,
		    new Param(ProcessManagerBind.processNameProp, processName));

		return pmb;
	}

	@Override
	public List<Actor> getAllGeneralProcessRoles() {

		@SuppressWarnings("unchecked")
		List<Actor> all = getEntityManager().createNamedQuery(
		    Actor.getAllGeneral).getResultList();

		return all;
	}

	@Override
	public List<Actor> getProcessRoles(Collection<Long> actorIds) {

		if (actorIds == null || actorIds.isEmpty())
			throw new IllegalArgumentException("ActorIds should contain values");

		@SuppressWarnings("unchecked")
		List<Actor> all = getEntityManager().createNamedQuery(
		    Actor.getAllByActorIds).setParameter(Actor.actorIdProperty,
		    actorIds).getResultList();

		return all;
	}

	@Override
	@Transactional(readOnly = false)
	public void updateAddGrpsToRole(Long roleActorId,
	        Collection<String> selectedGroupsIds) {

		Actor roleIdentity = find(Actor.class, roleActorId);

		List<NativeIdentityBind> nativeIdentities = new ArrayList<NativeIdentityBind>(
		        selectedGroupsIds.size());

		for (String groupId : selectedGroupsIds) {

			NativeIdentityBind nativeIdentity = new NativeIdentityBind();
			nativeIdentity.setIdentityId(groupId);
			nativeIdentity.setIdentityType(IdentityType.GROUP);
			nativeIdentity.setActor(roleIdentity);
			nativeIdentities.add(nativeIdentity);
		}

		List<NativeIdentityBind> existingNativeIdentities = roleIdentity
		        .getNativeIdentities();
		List<Long> nativeIdentitiesToRemove = new ArrayList<Long>();

		if (existingNativeIdentities != null) {

			for (NativeIdentityBind existing : existingNativeIdentities) {

				if (nativeIdentities.contains(existing)) {

					nativeIdentities.remove(existing);
					nativeIdentities.add(existing);
				} else {

					nativeIdentitiesToRemove.add(existing.getId());
				}
			}
		} else {
			existingNativeIdentities = new ArrayList<NativeIdentityBind>();
		}

		roleIdentity.setNativeIdentities(nativeIdentities);
		getEntityManager().merge(roleIdentity);

		if (!nativeIdentitiesToRemove.isEmpty())
			getEntityManager().createNamedQuery(NativeIdentityBind.deleteByIds)
			        .setParameter(NativeIdentityBind.idsParam,
			            nativeIdentitiesToRemove).executeUpdate();
	}

	@Override
	public List<NativeIdentityBind> getNativeIdentities(
	        long processRoleIdentityId) {

		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(
		    NativeIdentityBind.getByProcIdentity).setParameter(
		    NativeIdentityBind.procIdentityParam, processRoleIdentityId)
		        .getResultList();

		return binds;
	}

	@Override
	public List<NativeIdentityBind> getNativeIdentities(
	        Collection<Long> actorsIds, IdentityType identityType) {

		@SuppressWarnings("unchecked")
		List<NativeIdentityBind> binds = getEntityManager().createNamedQuery(
		    NativeIdentityBind.getByTypesAndProceIdentities).setParameter(
		    NativeIdentityBind.identityTypeProperty, identityType)
		        .setParameter(Actor.actorIdProperty, actorsIds).getResultList();

		return binds;
	}

	@Override
	@Transactional(readOnly = false)
	public void updateCreateProcessRoles(Collection<Role> rolesNames,
	        Long processInstanceId) {

		for (Role role : rolesNames) {

			Actor prole = new Actor();
			prole.setProcessRoleName(role.getRoleName());
			prole.setProcessInstanceId(processInstanceId);

			persist(prole);
		}
	}

	@Override
	public List<Object[]> getProcessTasksViewsInfos(
	        Collection<Long> processDefinitionsIds, String viewType) {

		if (processDefinitionsIds == null || processDefinitionsIds.isEmpty()
		        || viewType == null)
			return new ArrayList<Object[]>(0);

		@SuppressWarnings("unchecked")
		List<Object[]> viewsInfos = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_PROCESS_TASK_VIEW_INFO).setParameter(
		    ViewTaskBind.processDefIdsParam, processDefinitionsIds)
		        .setParameter(ViewTaskBind.viewTypeProp, viewType)
		        .getResultList();

		return viewsInfos;
	}

	@Override
	public List<Actor> getProcessRoles(Collection<String> rolesNames,
	        Long processInstanceId) {

		List<Actor> proles = getResultList(Actor.getSetByRoleNamesAndPIId,
		    Actor.class, new Param(Actor.processRoleNameProperty, rolesNames),
		    new Param(Actor.processInstanceIdProperty, processInstanceId));

		return proles;
	}

	@Override
	public List<ProcessInstance> getSubprocessInstancesOneLevel(long parentProcessInstanceId) {
		List<ProcessInstance> subprocesses = null;

		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("load_bpm_subproc_by_hib", Boolean.TRUE)) {
			getResultList(ProcessManagerBind.getSubprocessesOneLevel, ProcessInstance.class,
					new Param(ProcessManagerBind.processInstanceIdParam, parentProcessInstanceId));
			if (!ListUtil.isEmpty(subprocesses))
				return subprocesses;
		}

		String query = "select p.id_ from jbpm_processinstance p, jbpm_token t where t.id_ = p.SUPERPROCESSTOKEN_ and t.PROCESSINSTANCE_ = " +
				parentProcessInstanceId;
		LOGGER.info("No sub-processes were found by named query " + ProcessManagerBind.getSubprocessesOneLevel + ", will try to find them by query " +
				query);
		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(results))
			return Collections.emptyList();

		Map<Long, Boolean> piIds = new HashMap<Long, Boolean>();
		for (Serializable[] data: results) {
			if (ArrayUtil.isEmpty(data))
				continue;

			Serializable id = data[0];
			if (id instanceof Number)
				piIds.put(((Number) id).longValue(), Boolean.TRUE);
		}
		if (MapUtil.isEmpty(piIds))
			return Collections.emptyList();

		LOGGER.info("Found sub-processes " + piIds.keySet() + " for given process " + parentProcessInstanceId);

		query = "from " + org.jbpm.graph.exe.ProcessInstance.class.getName() + " p where p.id in (:" + ProcessManagerBind.processInstanceIdParam + ")";
		subprocesses = getResultListByInlineQuery(query, ProcessInstance.class,
				new Param(ProcessManagerBind.processInstanceIdParam, new ArrayList<Long>(piIds.keySet())));

		return subprocesses;
	}

	@Override
	public ProcessDefinition findLatestProcessDefinition(final String processName) {
		return getBpmContext().execute(new JbpmCallback<ProcessDefinition>() {

			@Override
			public ProcessDefinition doInJbpm(JbpmContext context) throws JbpmException {
				return context.getGraphSession().findLatestProcessDefinition(processName);
			}
		});
	}

	BPMContext getBpmContext() {
		return bpmContext;
	}

	@Override
	public List<ActorPermissions> getPermissionsForUser(Integer userId, String processName, Long processInstanceId, Set<String> userNativeRoles,
			Set<String> userGroupsIds) {

		if (userGroupsIds != null) {
			throw new UnsupportedOperationException("Searching by user groups not supported yet");
		}
		if (processName != null) {
			throw new UnsupportedOperationException("Searching by process name not supported yet");
		}

		if (ListUtil.isEmpty(userNativeRoles)) {
			//	This is perhaps silly, but just because we don't want to maintain two queries just for the case, when user doesn't have any roles
			//	this is the case only for bpmUser usually
			userNativeRoles = new HashSet<String>(1);
			userNativeRoles.add("mock2345324659324");
		}

		String identityTypeRoleParam = "identityTypeRole";
		String identityIdsRolesParam = "identityIdsRoles";

		List<ActorPermissions> perms = getResultListByInlineQuery(
		    "select perms from com.idega.jbpm.data.Actor a inner join a."+ Actor.nativeIdentitiesProperty+ " ni inner join a."+Actor.actorPermissionsProperty+" perms "+
		    "where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+" and " +
		    		"((ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty+" and ni."+NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+") " +
		    		"or (ni."+NativeIdentityBind.identityTypeProperty+" = :"+identityTypeRoleParam+" and ni."+NativeIdentityBind.identityIdProperty+" in (:"+identityIdsRolesParam+")))"

		            , ActorPermissions.class,
		            new Param(Actor.processInstanceIdProperty, processInstanceId),
		            new Param(NativeIdentityBind.identityTypeProperty, NativeIdentityBind.IdentityType.USER),
		            new Param(NativeIdentityBind.identityIdProperty, String.valueOf(userId)),
		            new Param(identityTypeRoleParam, NativeIdentityBind.IdentityType.ROLE),
		            new Param(identityIdsRolesParam, userNativeRoles));

		List<Actor> globalRolesActors = getResultListByInlineQuery(
		    "select a from com.idega.jbpm.data.Actor a "+
		    "where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+
		    " and a."+Actor.processRoleNameProperty+" in(:"+Actor.processRoleNameProperty+")"
		            , Actor.class,
		            new Param(Actor.processInstanceIdProperty, processInstanceId),
		            new Param(Actor.processRoleNameProperty, userNativeRoles)
		);

		if (globalRolesActors != null) {
			for (Actor actor : globalRolesActors) {
				if (ListUtil.isEmpty(actor.getNativeIdentities())) {
					if (actor.getActorPermissions() != null) {
						perms.addAll(actor.getActorPermissions());
					}
				}
	        }
		}

		return perms;
	}

	@Override
	public int getTaskViewBindCount(String viewId, String viewType) {
		return getResultList(ViewTaskBind.GET_VIEW_TASK_BIND_BY_VIEW_QUERY_NAME,
			    ViewTaskBind.class, new Param(ViewTaskBind.viewIdParam, viewId),
			    new Param(ViewTaskBind.viewTypeParam, viewType)).size();
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

	//	TODO: very likely this method should be removed: the variables are bound dynamically as a process instance is in action
	@Override
	public void bindProcessVariables() {
		try {
			List<AutoloadedProcessDefinition> procDefs = getAllLoadedProcessDefinitions();
			if (ListUtil.isEmpty(procDefs)) {
				return;
			}

			List<ProcessDefinitionVariablesBind> currentBinds = getAllProcDefVariableBinds();

			for (AutoloadedProcessDefinition apd: procDefs) {
				String processDefinitionName = apd.getProcessDefinitionName();
				@SuppressWarnings("deprecation")
				Collection<VariableInstanceInfo> currentVariables = getVariablesQuerier().getVariablesByProcessDefinitionNaiveWay(processDefinitionName);
				bindProcessVariables(processDefinitionName, currentBinds, currentVariables, null);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error binding process variables", e);
		}
	}

	private void bindProcessVariables(String processDefinitionName, Long processInstanceId, Set<String> createdVariables) {
		bindProcessVariables(processDefinitionName, getAllProcDefVariableBinds(), getVariablesQuerier().getVariablesByProcessInstanceId(processInstanceId),
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

	private ProcessDefinitionVariablesBind createProcessDefinitionVariablesBind(List<ProcessDefinitionVariablesBind> currentBinds, String processDefinitionName,
			String variableName, String variableType) {
		try {
			if (bindExists(currentBinds, variableName, processDefinitionName)) {
				return null;
			}
			ProcessDefinitionVariablesBind bind = new ProcessDefinitionVariablesBind();
			if (bind.hashCode() < 0) {
				return null;
			}

			bind.setProcessDefinition(processDefinitionName);
			bind.setVariableName(variableName);
			bind.setVariableType(variableType);
			persist(bind);

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

	VariableInstanceQuerier getVariablesQuerier() {
		return variablesQuerier;
	}

	@Override
	public void onApplicationEvent(final VariableCreatedEvent event) {
		if (event instanceof VariableCreatedEvent) {
			Thread binder = new Thread(new Runnable() {
				@Override
				public void run() {
					VariableCreatedEvent variableCreated = event;
					Map<String, Object> createdVariables = variableCreated.getVariables();
					bindProcessVariables(variableCreated.getProcessDefinitionName(), variableCreated.getProcessInstanceId(),
							createdVariables == null ? null : createdVariables.keySet());
				}
			});
			binder.start();
		}
	}

	protected List<Long> getAllProcessInstances() {
		try {
			return getResultListByInlineQuery("select pi.id from org.jbpm.graph.exe.ProcessInstance pi", Long.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting all process instances", e);
		}
		return null;
	}

	protected List<Long> getExisitingVariables() {
		try {
			return getResultListByInlineQuery("select distinct var.variableId from " + BPMVariableData.class.getName() + " var", Long.class);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting all process instances", e);
		}
		return null;
	}

	@Override
	public void importVariablesData() {
		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
		String property = "jbpm_vars_data_imported";
		if (settings.getBoolean(property, Boolean.FALSE)) {
			return;
		}

		prepareTable();

		settings.setProperty(property, Boolean.TRUE.toString());
	}

	private void prepareTable() {
		boolean oracle = isOracle();
		if (oracle) {
			createSequence(BPMVariableData.TABLE_NAME);
		}

		createIndex(BPMVariableData.TABLE_NAME, "IDX_" + BPMVariableData.TABLE_NAME + "_VAR", BPMVariableData.COLUMN_VARIABLE_ID);
		createIndex(BPMVariableData.TABLE_NAME, "IDX_" + BPMVariableData.TABLE_NAME + "_VAL", BPMVariableData.COLUMN_VALUE);

		String refNew = getTriggerReference("NEW");
		createTrigger("CREATE TRIGGER BPM_VARIABLE_INSERTED AFTER INSERT ON JBPM_VARIABLEINSTANCE " + (oracle ? "referencing new as new ": CoreConstants.EMPTY) +
						"FOR EACH ROW\n" +
						"BEGIN\n" +
							"IF (" + refNew + ".ID_ is not null and " + refNew + ".stringvalue_ is not null) THEN\n" +
								"insert into BPM_VARIABLE_DATA (" + (oracle ? "id, " : CoreConstants.EMPTY) + "variable_id, stringvalue) values (" +
									(oracle ? BPMVariableData.TABLE_NAME + "_seq.NEXTVAL, " : CoreConstants.EMPTY) + refNew + ".ID_, substr(" + refNew +
																																	".stringvalue_, 1, 255));\n" +
							"END IF;\n"+
						"END;"
		);
		createTrigger("CREATE TRIGGER BPM_VARIABLE_UPDATED AFTER UPDATE ON JBPM_VARIABLEINSTANCE " + (oracle ? "referencing new as new ": CoreConstants.EMPTY) +
						"FOR EACH ROW\n" +
						"BEGIN\n" +
							"update BPM_VARIABLE_DATA set stringvalue=substr("+refNew+".stringvalue_, 1, 255) where variable_id="+refNew+".ID_;\n" +
						"END;"
		);
		String refOld = getTriggerReference("OLD");
		createTrigger("CREATE TRIGGER BPM_VARIABLE_DELETED BEFORE DELETE ON JBPM_VARIABLEINSTANCE " + (oracle ? "referencing old as old ": CoreConstants.EMPTY) +
						"FOR EACH ROW\n" +
						"BEGIN\n" +
							"delete from BPM_VARIABLE_DATA where variable_id="+refOld+".ID_;\n" +
						"END;"
		);
	}

	private void createSequence(String tableName) {
		try {
			DatastoreInterface dataStore = DatastoreInterface.getInstance();
			dataStore.createSequence(tableName, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error creating sequence for table: " + tableName, e);
		}
	}

	private boolean isOracle() {
		DatastoreInterface dataStore = DatastoreInterface.getInstance();
		return dataStore instanceof OracleDatastoreInterface;
	}

	private String getTriggerReference(String reference) {
		if (isOracle()) {
			reference = ":".concat(reference);
			reference = reference.toLowerCase();
		}
		return reference;
	}

	private void createIndex(String table, String name, String column) {
		try {
			DatastoreInterface dataInterface = DatastoreInterface.getInstance();
			dataInterface.createIndex(table, name, new String[] {column});
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error creating index '" + name + "' for table '" + table + "' and column: '" + column + "'", e);
		}
	}

	private void createTrigger(String triggerSQL) {
		try {
			DatastoreInterface dataInterface = DatastoreInterface.getInstance();
			dataInterface.createTrigger(triggerSQL);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error creating trigger: " + triggerSQL, e);
		}
	}

	@Override
	public List<Long> getProcessInstanceIdsByProcessDefinitionNames(List<String> processDefinitionNames) {
		if (ListUtil.isEmpty(processDefinitionNames)) {
			return Collections.emptyList();
		}

		return getResultListByInlineQuery("select pi.id from " + ProcessInstance.class.getName() + " pi, " + ProcessDefinition.class.getName() +
				" pd where pi.processDefinition = pd.id and pd.name = :processDefinitionNames", Long.class, new Param("processDefinitionNames", processDefinitionNames));
	}

	private Map<Long, Map<String, java.util.Date>> getProcInstIdsByDateRangeAndProcDefNamesOrProcInstIdsUsingCases(IWTimestamp from, IWTimestamp to,
			List<String> procDefNames, List<Long> procInsIds) {

		if (from == null && to == null)
			return null;

		String fromSnippet = CoreConstants.EMPTY;
		if (from != null)
			fromSnippet = " c." + CaseBMPBean.COLUMN_CREATED + " >= '" + from.getDateString("yyyy-MM-dd HH:mm:ss") + "' and ";
		String toSnippet = CoreConstants.EMPTY;
		if (to != null)
			toSnippet = " c." + CaseBMPBean.COLUMN_CREATED + " <= '" + to.getDateString("yyyy-MM-dd HH:mm:ss") + "' and ";

		String query = "select distinct p.process_instance_id, c." + CaseBMPBean.COLUMN_CREATED + ", pi.end_ from BPM_CASES_PROCESSINSTANCES p, "
				+ CaseBMPBean.TABLE_NAME + " c, jbpm_processinstance pi ";

		String procDefSnippet = CoreConstants.EMPTY;
		if (!ListUtil.isEmpty(procDefNames)) {
			query += ", jbpm_processdefinition pd ";
			procDefSnippet = " pd.name_ in (";
			for (Iterator<String> procDefNamesIter = procDefNames.iterator(); procDefNamesIter.hasNext();) {
				procDefSnippet = procDefSnippet.concat(CoreConstants.QOUTE_SINGLE_MARK).concat(procDefNamesIter.next())
						.concat(CoreConstants.QOUTE_SINGLE_MARK);
				if (procDefNamesIter.hasNext())
					procDefSnippet = procDefSnippet.concat(CoreConstants.COMMA).concat(CoreConstants.SPACE);
			}
			procDefSnippet += ") and pd.id_ = pi.PROCESSDEFINITION_ ";
		}

		query += " where " + fromSnippet + toSnippet + procDefSnippet + " and pi.id_ = p.process_instance_id ";

		if (!ListUtil.isEmpty(procInsIds)) {
			query = query + " and p.process_instance_id in ";
			StringBuffer procInstIdsExpression = new StringBuffer();
			for (Iterator<Long> idsIter = procInsIds.iterator(); idsIter.hasNext();) {
				procInstIdsExpression.append(idsIter.next());
				if (idsIter.hasNext())
					procInstIdsExpression.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
			}
			query = query + procInstIdsExpression.toString();
		}

		query = query.trim();
		if (!query.endsWith("and"))
			query += " and";
		query += " p.case_id = c." + CaseBMPBean.PK_COLUMN + " order by c." + CaseBMPBean.COLUMN_CREATED + " desc";

		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, 3);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(results))
			return null;

		return getIdsWithDates(results);
	}

	@Override
	public Map<Long, Map<String, java.util.Date>> getProcessInstanceIdsByDateRangeAndProcessDefinitionNamesOrProcInstIds(
			IWTimestamp from,
			IWTimestamp to,
			List<String> processDefinitionNames,
			List<Long> procInsIds) {

		if (from == null && to == null) {
			LOGGER.warning("Both from and to dates are not defined, unable to get proc. inst. IDs by date range!");
			return null;
		}

		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("use_new_date_ranger_querier", Boolean.TRUE))
			return getProcInstIdsByDateRangeAndProcDefNamesOrProcInstIdsUsingCases(from, to, processDefinitionNames, procInsIds);

		List<Param> params = new ArrayList<Param>();
		if (from != null)
			params.add(new Param("piFrom", new java.util.Date(from.getTimestamp().getTime())));
		if (to != null)
			params.add(new Param("piTo", new java.util.Date(to.getTimestamp().getTime())));
		boolean byProcInst = !ListUtil.isEmpty(procInsIds);
		if (byProcInst)
			params.add(new Param("procInstIds", procInsIds));
		else
			params.add(new Param("procDefNames", processDefinitionNames));

		StringBuilder query = new StringBuilder("SELECT pi.id, pi.start, pi.end FROM ").append(ProcessInstance.class.getName())
				.append(" pi").append((byProcInst ?
				" where pi.id in (:procInstIds)"
				: ", " 	+ ProcessDefinition.class.getName()
						+ " pd where pd.name in (:procDefNames) and pi.processDefinition = pd.id")
		).append((from == null ? CoreConstants.EMPTY : " and pi.start >= :piFrom")
		).append((to == null ? CoreConstants.EMPTY : " and pi.start <= :piTo"));

		List<Serializable[]> results = null;
		try {
			results = getResultListByInlineQuery(query.toString(), Serializable[].class, ArrayUtil.convertListToArray(params));
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting process instance IDs by"
					+ " dates: from=" + from + ", to=" + to
					+ ", proc. def. names=" + processDefinitionNames
					+ ", proc. inst. IDs=" + procInsIds + "\nQuery: " + query.toString(), e);
		}
		return getIdsWithDates(results);
	}

	private Map<Long, Map<String, java.util.Date>> getIdsWithDates(List<Serializable[]> results) {
		if (ListUtil.isEmpty(results))
			return null;

		Map<Long, Map<String, java.util.Date>> idsWithDates = new LinkedHashMap<Long, Map<String, java.util.Date>>();
		for (Object[] result: results) {
			if (result.length != 3) {
				LOGGER.warning("Not enough data to construct result");
				continue;
			}

			Long procInstId = null;
			Map<String, java.util.Date> dates = new HashMap<String, java.util.Date>();
			for (int i = 0; i < result.length; i++) {
				Object value = result[i];
				if (i == 0 && value instanceof Number) {
					procInstId = ((Number) value).longValue();
					idsWithDates.put(procInstId, dates);
				} else if (value instanceof java.util.Date) {
					dates.put(i == 1 ? "start" : "end", (java.util.Date) value);
				}
			}
		}

		return idsWithDates;
	}

	@Override
	public List<Object[]> getProcessDateRanges(Collection<Long> processInstanceIds) {
		return getProcessDateRanges(ListUtil.isEmpty(processInstanceIds) ? null : new ArrayList<Long>(processInstanceIds), null);
	}

	private List<Object[]> getProcessDateRanges(List<Long> processInstanceIds, List<Object[]> results) {
		if (ListUtil.isEmpty(processInstanceIds))
			return results;

		List<Long> usedIds = null;
		if (processInstanceIds.size() > 1000) {
			usedIds = new ArrayList<Long>(processInstanceIds.subList(0, 1000));
			processInstanceIds = new ArrayList<Long>(processInstanceIds.subList(1000, processInstanceIds.size()));
		} else {
			usedIds = new ArrayList<Long>(processInstanceIds);
			processInstanceIds = null;
		}

		List<Object[]> temp = getResultListByInlineQuery("select p.id, p.start, p.end from " + ProcessInstance.class.getName() +
				" p where p.id in (:processInstanceIds)", Object[].class, new Param("processInstanceIds", usedIds));

		if (results == null)
			results = new ArrayList<Object[]>();
		if (!ListUtil.isEmpty(temp))
			results.addAll(temp);

		return getProcessDateRanges(processInstanceIds, results);
	}

	@Override
	public String getProcessDefinitionNameByProcessDefinitionId(Long processDefinitionId) {
		if (processDefinitionId == null)
			return null;

		return getSingleResultByInlineQuery("select pd.name from " + ProcessDefinition.class.getName() + " pd where pd.id = :processDefinitionId", String.class,
				new Param("processDefinitionId", processDefinitionId));
	}

	@Override
	public List<Long> getProcessDefinitionIdsByName(String procDefName) {
		if (StringUtil.isEmpty(procDefName))
			return null;

		return getResultListByInlineQuery("select pd.id from " + ProcessDefinition.class.getName() + " pd where pd.name = :procDefName", Long.class,
				new Param("procDefName", procDefName));
	}
}