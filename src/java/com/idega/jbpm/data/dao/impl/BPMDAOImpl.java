package com.idega.jbpm.data.dao.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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

import javax.servlet.ServletContext;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.process.data.CaseBMPBean;
import com.idega.core.accesscontrol.data.bean.ICRole;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.DatastoreInterface;
import com.idega.data.OracleDatastoreInterface;
import com.idega.data.SimpleQuerier;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.business.BPMAssetsResolver;
import com.idega.jbpm.data.Actor;
import com.idega.jbpm.data.ActorPermissions;
import com.idega.jbpm.data.BPMVariableData;
import com.idega.jbpm.data.NativeIdentityBind;
import com.idega.jbpm.data.NativeIdentityBind.IdentityType;
import com.idega.jbpm.data.ProcessManagerBind;
import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.VariableBytes;
import com.idega.jbpm.data.ViewTaskBind;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.identity.Role;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
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
public class BPMDAOImpl extends GenericDaoImpl implements BPMDAO {

	private static final Logger LOGGER = Logger.getLogger(BPMDAOImpl.class.getName());

	@Autowired
	private BPMContext bpmContext;

	@Override
	public ViewTaskBind getViewTaskBind(long taskId, String viewType) {
		if (viewType.length() > ICRole.ROLE_KEY_MAX_LENGTH) {
			String tmp = viewType.substring(0, ICRole.ROLE_KEY_MAX_LENGTH);
			LOGGER.warning("Had to shorten view type param '" + viewType + "' to '" + tmp + "'");
			viewType = tmp;
		}

		@SuppressWarnings("unchecked")
		List<ViewTaskBind> binds = getEntityManager().createNamedQuery(
		    ViewTaskBind.GET_UNIQUE_BY_TASK_ID_AND_VIEW_TYPE_QUERY_NAME)
		        .setParameter(ViewTaskBind.taskIdParam, taskId)
		        .setParameter(ViewTaskBind.viewTypeParam, viewType)
		    .getResultList();

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

		if (actorIds == null || actorIds.isEmpty()) {
			throw new IllegalArgumentException("ActorIds should contain values");
		}

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

		if (!nativeIdentitiesToRemove.isEmpty()) {
			getEntityManager().createNamedQuery(NativeIdentityBind.deleteByIds)
			        .setParameter(NativeIdentityBind.idsParam,
			            nativeIdentitiesToRemove).executeUpdate();
		}
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
		        || viewType == null) {
			return new ArrayList<Object[]>(0);
		}

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
	public List<ProcessInstance> getRootProcesses(Long procInstId) {
		if (procInstId == null) {
			return null;
		}

		try {
			return getResultList(
					ProcessManagerBind.getRootProcessesOneLevel,
					ProcessInstance.class,
					new Param(ProcessManagerBind.processInstanceIdParam, procInstId)
			);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting root processes by proc. inst. ID: " + procInstId, e);
		}

		return null;
	}

	@Override
	public List<ProcessInstance> getSubprocessInstancesOneLevel(long parentProcessInstanceId) {
		List<ProcessInstance> subprocesses = null;

		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("load_bpm_subproc_by_hib", Boolean.TRUE)) {
			subprocesses = getResultList(
					ProcessManagerBind.getSubprocessesOneLevel,
					ProcessInstance.class,
					new Param(ProcessManagerBind.processInstanceIdParam, parentProcessInstanceId)
			);
			if (!ListUtil.isEmpty(subprocesses)) {
				return subprocesses;
			}
		}

		String query = "select p.id_ from jbpm_processinstance p, jbpm_token t where t.id_ = p.SUPERPROCESSTOKEN_ and t.PROCESSINSTANCE_ = " + parentProcessInstanceId;
		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, 1);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(results)) {
			return Collections.emptyList();
		}

		Map<Long, Boolean> piIds = new HashMap<Long, Boolean>();
		for (Serializable[] data: results) {
			if (ArrayUtil.isEmpty(data)) {
				continue;
			}

			Serializable id = data[0];
			if (id instanceof Number) {
				piIds.put(((Number) id).longValue(), Boolean.TRUE);
			}
		}
		if (MapUtil.isEmpty(piIds)) {
			return Collections.emptyList();
		}

		query = "from " + org.jbpm.graph.exe.ProcessInstance.class.getName() + " p where p.id in (:" + ProcessManagerBind.processInstanceIdParam + ")";
		subprocesses = getResultListByInlineQuery(
				query,
				ProcessInstance.class,
				new Param(ProcessManagerBind.processInstanceIdParam,new ArrayList<Long>(piIds.keySet()))
		);

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
		    "select perms from com.idega.jbpm.data.Actor a inner join a."+ Actor.nativeIdentitiesProperty+ " ni inner join a." +
		    		Actor.actorPermissionsProperty+" perms "+
		    "where a."+Actor.processInstanceIdProperty+" = :"+Actor.processInstanceIdProperty+" and " +
		    		"((ni."+NativeIdentityBind.identityTypeProperty+" = :"+NativeIdentityBind.identityTypeProperty+" and ni." +
		    		NativeIdentityBind.identityIdProperty+" = :"+NativeIdentityBind.identityIdProperty+") " +
		    		"or (ni."+NativeIdentityBind.identityTypeProperty+" = :"+identityTypeRoleParam+" and ni."+NativeIdentityBind.identityIdProperty +
		    			" in (:"+identityIdsRolesParam+")))"

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
		createTrigger("CREATE TRIGGER BPM_VARIABLE_INSERTED AFTER INSERT ON JBPM_VARIABLEINSTANCE " +
				(oracle ? "referencing new as new ": CoreConstants.EMPTY) +
						"FOR EACH ROW\n" +
						"BEGIN\n" +
							"IF (" + refNew + ".ID_ is not null and " + refNew + ".stringvalue_ is not null) THEN\n" +
								"insert into BPM_VARIABLE_DATA (" + (oracle ? "id, " : CoreConstants.EMPTY) + "variable_id, stringvalue) values (" +
									(oracle ? BPMVariableData.TABLE_NAME + "_seq.NEXTVAL, " : CoreConstants.EMPTY) + refNew + ".ID_, substr(" +
										refNew + ".stringvalue_, 1, 255));\n" +
							"END IF;\n"+
						"END;"
		);
		createTrigger("CREATE TRIGGER BPM_VARIABLE_UPDATED AFTER UPDATE ON JBPM_VARIABLEINSTANCE " +
				(oracle ? "referencing new as new ": CoreConstants.EMPTY) +
						"FOR EACH ROW\n" +
						"BEGIN\n" +
							"update BPM_VARIABLE_DATA set stringvalue=substr("+refNew+".stringvalue_, 1, 255) where variable_id="+refNew+".ID_;\n" +
						"END;"
		);
		String refOld = getTriggerReference("OLD");
		createTrigger("CREATE TRIGGER BPM_VARIABLE_DELETED BEFORE DELETE ON JBPM_VARIABLEINSTANCE " +
				(oracle ? "referencing old as old ": CoreConstants.EMPTY) +
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
				" pd where pi.processDefinition = pd.id and pd.name = :processDefinitionNames", Long.class,
				new Param("processDefinitionNames", processDefinitionNames));
	}

	private Map<Long, Map<String, java.util.Date>> getProcInstIdsByDateRangeAndProcDefNamesOrProcInstIdsUsingCases(IWTimestamp from, IWTimestamp to,
			List<String> procDefNames, List<Long> procInsIds) {

		if (from == null && to == null) {
			return null;
		}

		String fromSnippet = CoreConstants.EMPTY;
		if (from != null) {
			fromSnippet = " c." + CaseBMPBean.COLUMN_CREATED + " >= '" + from.getDateString("yyyy-MM-dd HH:mm:ss") + "' and ";
		}
		String toSnippet = CoreConstants.EMPTY;
		if (to != null) {
			toSnippet = " c." + CaseBMPBean.COLUMN_CREATED + " <= '" + to.getDateString("yyyy-MM-dd HH:mm:ss") + "' and ";
		}

		String query = "select distinct p.process_instance_id, c." + CaseBMPBean.COLUMN_CREATED + ", pi.end_ from BPM_CASES_PROCESSINSTANCES p, "
				+ CaseBMPBean.TABLE_NAME + " c, jbpm_processinstance pi ";

		String procDefSnippet = CoreConstants.EMPTY;
		if (!ListUtil.isEmpty(procDefNames)) {
			query += ", jbpm_processdefinition pd ";
			procDefSnippet = " pd.name_ in (";
			for (Iterator<String> procDefNamesIter = procDefNames.iterator(); procDefNamesIter.hasNext();) {
				procDefSnippet = procDefSnippet.concat(CoreConstants.QOUTE_SINGLE_MARK).concat(procDefNamesIter.next())
						.concat(CoreConstants.QOUTE_SINGLE_MARK);
				if (procDefNamesIter.hasNext()) {
					procDefSnippet = procDefSnippet.concat(CoreConstants.COMMA).concat(CoreConstants.SPACE);
				}
			}
			procDefSnippet += ") and pd.id_ = pi.PROCESSDEFINITION_ ";
		}

		query += " where " + fromSnippet + toSnippet + procDefSnippet + " and pi.id_ = p.process_instance_id ";

		if (!ListUtil.isEmpty(procInsIds)) {
			query = query + " and p.process_instance_id in ";
			StringBuffer procInstIdsExpression = new StringBuffer();
			for (Iterator<Long> idsIter = procInsIds.iterator(); idsIter.hasNext();) {
				procInstIdsExpression.append(idsIter.next());
				if (idsIter.hasNext()) {
					procInstIdsExpression.append(CoreConstants.COMMA).append(CoreConstants.SPACE);
				}
			}
			query = query + procInstIdsExpression.toString();
		}

		query = query.trim();
		if (!query.endsWith("and")) {
			query += " and";
		}
		query += " p.case_id = c." + CaseBMPBean.PK_COLUMN + " order by c." + CaseBMPBean.COLUMN_CREATED + " desc";

		List<Serializable[]> results = null;
		try {
			results = SimpleQuerier.executeQuery(query, 3);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error executing query: " + query, e);
		}
		if (ListUtil.isEmpty(results)) {
			return null;
		}

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

		if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("use_new_date_ranger_querier", Boolean.TRUE)) {
			return getProcInstIdsByDateRangeAndProcDefNamesOrProcInstIdsUsingCases(from, to, processDefinitionNames, procInsIds);
		}

		List<Param> params = new ArrayList<Param>();
		if (from != null) {
			params.add(new Param("piFrom", new java.util.Date(from.getTimestamp().getTime())));
		}
		if (to != null) {
			params.add(new Param("piTo", new java.util.Date(to.getTimestamp().getTime())));
		}
		boolean byProcInst = !ListUtil.isEmpty(procInsIds);
		if (byProcInst) {
			params.add(new Param("procInstIds", procInsIds));
		} else {
			params.add(new Param("procDefNames", processDefinitionNames));
		}

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
		if (ListUtil.isEmpty(results)) {
			return null;
		}

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
		if (ListUtil.isEmpty(processInstanceIds)) {
			return results;
		}

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

		if (results == null) {
			results = new ArrayList<Object[]>();
		}
		if (!ListUtil.isEmpty(temp)) {
			results.addAll(temp);
		}

		return getProcessDateRanges(processInstanceIds, results);
	}

	@Override
	public String getProcessDefinitionNameByProcessDefinitionId(Long processDefinitionId) {
		if (processDefinitionId == null) {
			return null;
		}

		return getSingleResultByInlineQuery("select pd.name from " + ProcessDefinition.class.getName() + " pd where pd.id = :processDefinitionId",
				String.class,
				new Param("processDefinitionId", processDefinitionId));
	}

	@Override
	public List<Long> getProcessDefinitionIdsByName(String procDefName) {
		if (StringUtil.isEmpty(procDefName)) {
			return null;
		}

		return getResultListByInlineQuery("select pd.id from " + ProcessDefinition.class.getName() + " pd where pd.name = :procDefName", Long.class,
				new Param("procDefName", procDefName));
	}

	@Override
	@Transactional(readOnly = false)
	public Variable saveVariable(Long procInstId, Long taskInstId, Long tokenId, String name, Serializable value) {
		if (StringUtil.isEmpty(name) || value == null) {
			return null;
		}

		if (procInstId == null && taskInstId == null) {
			return null;
		}

		Variable var = new Variable();
		var.setProcessInstance(procInstId);
		var.setTaskInstance(taskInstId);
		var.setToken(tokenId);
		var.setVersion(0);
		var.setName(name);
		var.setValue(value);

		try {
			persist(var);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error saving variable: " + var, e);
		}

		return var == null || var.getId() == null ? null : var;
	}

	@Override
	@Transactional(readOnly = false)
	public boolean updateVariable(Long procInstId, String name, Serializable value) {
		if (procInstId == null || StringUtil.isEmpty(name) || value == null) {
			return false;
		}

		try {
			List<Variable> variables = getVariablesByNameAndProcessInstance(name, procInstId);
			if (ListUtil.isEmpty(variables)) {
				return true;
			}

			for (Variable variable: variables) {
				variable.setVersion(0);
				variable.setValue(value);
				try {
					merge(variable);
				} catch (Exception e) {
					getLogger().log(Level.WARNING, "Error updating variable " + variable + ", proc. inst. ID: " + procInstId, e);
				}
			}

			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error updating variable " + name + ", proc. inst. ID: " + procInstId, e);
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.dao.BPMDAO#getProcessInstances(java.util.List)
	 */
	@Override
	public List<Variable> getVariablesByNameAndProcessInstance(String name, Long piId) {
		if (StringUtil.isEmpty(name)) {
			getLogger().warning("Name ('" + name + "') is not provided");
			return null;
		}

		return getVariablesByNameAndProcessInstance(Arrays.asList(name), piId);
	}

	@Override
	public List<Variable> getVariablesByNameAndProcessInstance(List<String> names, Long piId) {
		if (ListUtil.isEmpty(names) || piId == null) {
			getLogger().warning("Names (" + names + ") or proc. inst. ID (" + piId + ") are not provided");
			return null;
		}

		List<Variable> vars = getResultList(Variable.QUERY_GET_BY_NAMES_AND_PROC_INST, Variable.class,
				new Param(Variable.PARAM_NAMES, names),
				new Param(Variable.PARAM_PROC_INST_ID, piId)
		);

		return getValidVariables(vars);
	}

	@Override
	@Transactional(readOnly = false)
	public boolean deleteVariables(List<String> names, Long piId) {
		if (ListUtil.isEmpty(names) || piId == null) {
			getLogger().warning("Names (" + names + ") or proc. inst. ID (" + piId + ") are not provided");
			return false;
		}

		try {
			List<Variable> vars = getResultListByInlineQuery(
					"from Variable v where v.name in (:" + Variable.PARAM_NAMES + ") and v.processInstance = :" + Variable.PARAM_PROC_INST_ID,
					Variable.class,
					new Param(Variable.PARAM_NAMES, names),
					new Param(Variable.PARAM_PROC_INST_ID, piId)
			);

			if (ListUtil.isEmpty(vars)) {
				getLogger().info("No variables found by name(s) " + names + " for proc. inst. with ID " + piId);
				return true;
			}

			for (Variable var: vars) {
				remove(var);
			}

			return true;
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error deleting variable(s) from proc. inst. ID: " + piId, e);
		}

		return false;
	}

	private List<Variable> getValidVariables(List<Variable> vars) {
		if (ListUtil.isEmpty(vars)) {
			return null;
		}

		List<Variable> validVars = new ArrayList<Variable>();
		for (Variable var: vars) {
			if (var.getValue() == null) {
				continue;
			}

			validVars.add(var);
		}
		return validVars;
	}

	@Override
	public List<Variable> getVariablesByNamesAndProcessInstanceIds(List<String> names, List<Long> piIds) {
		if (ListUtil.isEmpty(names) || ListUtil.isEmpty(piIds)) {
			getLogger().warning("Names (" + names + ") or proc. inst. IDs (" + piIds + ") are not provided");
			return null;
		}

		List<Variable> vars = getResultList(Variable.QUERY_GET_BY_NAMES_AND_PROC_INST_IDS, Variable.class,
				new Param(Variable.PARAM_NAMES, names),
				new Param(Variable.PARAM_PROC_INST_IDS, piIds)
		);

		return getValidVariables(vars);
	}

	@Override
	public List<ProcessInstance> getProcessInstances(List<String> processDefinitionNames) {
		if (ListUtil.isEmpty(processDefinitionNames)) {
			return Collections.emptyList();
		}

		return getResultListByInlineQuery(
				"SELECT pi FROM " + ProcessInstance.class.getName() + " pi, " +
				ProcessDefinition.class.getName() + " pd " +
				"WHERE pi.processDefinition = pd.id " +
				"AND pd.name = :processDefinitionNames",
				ProcessInstance.class,
				new Param("processDefinitionNames", processDefinitionNames));
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.dao.BPMDAO#getProcessInstancesByIDs(java.util.Collection)
	 */
	@Override
	public List<ProcessInstance> getProcessInstancesByIDs(
			Collection<Long> processInstanceIds) {
		if (ListUtil.isEmpty(processInstanceIds)) {
			return Collections.emptyList();
		}

		StringBuilder query = new StringBuilder();
		query.append("FROM ").append(ProcessInstance.class.getName()).append(" pi ")
		.append("WHERE pi.id IN (:processInstanceIDs)");

		return getResultListByInlineQuery(
				query.toString(),
				ProcessInstance.class,
				new Param("processInstanceIDs", processInstanceIds));
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.dao.BPMDAO#getProcessDefinitions(java.lang.String)
	 */
	@Override
	public List<ProcessDefinition> getProcessDefinitions(Collection<String> processDefinitionNames) {
		if (!ListUtil.isEmpty(processDefinitionNames)) {
			StringBuilder query = new StringBuilder();
			query.append("FROM ").append(ProcessDefinition.class.getName()).append(" pd ");
			query.append("WHERE pd.name IN (:procDefNames)");

			return getResultListByInlineQuery(
					query.toString(),
					ProcessDefinition.class,
					new Param("procDefNames", processDefinitionNames));
		}

		return Collections.emptyList();
	}

	@Override
	public List<Variable> getVariablesByConditions(List<String> names, List<Long> piIds, List<Long> tiIds, List<Long> viIds) {
		if (ListUtil.isEmpty(names) && ListUtil.isEmpty(piIds) && ListUtil.isEmpty(tiIds) && ListUtil.isEmpty(viIds)) {
			getLogger().warning("Parameters are not provided");
			return null;
		}

		String query = "from Variable v where";
		List<Param> params = new ArrayList<Param>();
		if (!ListUtil.isEmpty(viIds)) {
			query += " v.id in (:" + Variable.PARAM_IDS + ")";
			params.add(new Param(Variable.PARAM_IDS, viIds));
		}
		if (!ListUtil.isEmpty(tiIds)) {
			if (!ListUtil.isEmpty(params)) {
				query += " and";
			}

			query += " v.taskInstance in (:" + Variable.PARAM_TASK_INST_IDS + ")";
			params.add(new Param(Variable.PARAM_TASK_INST_IDS, tiIds));
		}
		if (!ListUtil.isEmpty(piIds)) {
			if (!ListUtil.isEmpty(params)) {
				query += " and";
			}

			query += " v.processInstance in (:" + Variable.PARAM_PROC_INST_IDS + ")";
			params.add(new Param(Variable.PARAM_PROC_INST_IDS, piIds));
		}
		if (!ListUtil.isEmpty(names)) {
			if (!ListUtil.isEmpty(params)) {
				query += " and";
			}

			query += " v.name in (:" + Variable.PARAM_NAMES + ")";
			params.add(new Param(Variable.PARAM_NAMES, names));
		}
		query += " order by v.id desc";

		try {
			return getResultListByInlineQuery(query, Variable.class, ArrayUtil.convertListToArray(params));
		} catch (Exception e) {
			String message = "Error querying by: '" + query + "'. Variables names: " + names + ", proc. inst. IDs: " + piIds + ", task inst. IDs: " +
					tiIds + ", var. inst. IDs: " + viIds;
			getLogger().log(Level.WARNING, message, e);
			CoreUtil.sendExceptionNotification(message, e);
		}

		return Collections.emptyList();
	}

	@Override
	public List<Variable> getVariablesByBytes(List<Long> varBytesIds) {
		if (ListUtil.isEmpty(varBytesIds)) {
			return null;
		}

		return getResultListByInlineQuery(
				"from " + Variable.class.getName() + " v where v.byteArrayValue in (:varBytesIds)",
				Variable.class,
				new Param("varBytesIds", varBytesIds)
		);
	}

	@Override
	public List<Variable> getVariablesByTokens(List<Long> tokensIds) {
		if (ListUtil.isEmpty(tokensIds)) {
			return null;
		}

		return getResultListByInlineQuery(
				"from " + Variable.class.getName() + " v where v.token in (:tokensIds)",
				Variable.class,
				new Param("tokensIds", tokensIds)
		);
	}

	@Override
	public List<User> getUsersConnectedToProcess(Long piId, String procDefName, Map<String, Object> variables) {
		ServletContext servletCtx = IWMainApplication.getDefaultIWMainApplication().getServletContext();
		WebApplicationContext webAppCtx = WebApplicationContextUtils.getWebApplicationContext(servletCtx);
		Map<String, BPMAssetsResolver> bpmAssetsResolvers = webAppCtx.getBeansOfType(BPMAssetsResolver.class);
		if (!MapUtil.isEmpty(bpmAssetsResolvers)) {
			if (MapUtil.isEmpty(variables)) {
				Collection<Variable> vars = getVariablesByConditions(null, Arrays.asList(piId), null, null);
				if (!ListUtil.isEmpty(vars)) {
					variables = new HashMap<String, Object>();
					for (Variable var: vars) {
						Serializable value = var.getValue();
						if (value != null) {
							variables.put(var.getName(), value);
						}
					}
				}
			}

			List<User> allUsers = new ArrayList<User>();
			for (BPMAssetsResolver resolver: bpmAssetsResolvers.values()) {
				List<User> usersConnectedToProcess = resolver.getUsersConectedToProcess(piId, procDefName, variables);
				if (!ListUtil.isEmpty(usersConnectedToProcess)) {
					allUsers.addAll(usersConnectedToProcess);
				}
			}
			return ListUtil.isEmpty(allUsers) ? null : allUsers;
		}
		return null;
	}

	@Override
	public List<Token> getProcessTokens(Long piId) {
		if (piId == null) {
			return null;
		}

		return getResultListByInlineQuery(
				"from " + Token.class.getName() + " t where t.processInstance.id = :piId",
				Token.class,
				new Param("piId", piId)
		);
	}

	@Override
	public List<Long> getSubProcInstIdsByParentProcInstIdAndProcDefName(Long piId, String procDefName) {
		return getResultListByInlineQuery(
				"select t.subProcessInstance.id from " + Token.class.getName() + " t where t.processInstance.id = :piId and " +
						"t.subProcessInstance is not null and t.subProcessInstance.processDefinition.name = :procDefName",
				Long.class,
				new Param("piId", piId),
				new Param("procDefName", procDefName)
		);
	}

	@Override
	public List<Long> getIdsOfFinishedTaskInstancesForTask(Long piId, String taskName) {
		if (piId == null) {
			return null;
		}

		Map<Long, List<Long>> results = getIdsOfFinishedTaskInstancesForTask(Arrays.asList(piId), taskName);
		return MapUtil.isEmpty(results) ? null : results.get(piId);
	}

	@Override
	public Map<Long, List<Long>> getIdsOfFinishedTaskInstancesForTask(List<Long> procInstIds, String taskName) {
		if (ListUtil.isEmpty(procInstIds) || StringUtil.isEmpty(taskName)) {
			return null;
		}

		List<Object[]> allData = getResultListByInlineQuery(
				"select ti.processInstance.id, ti.id from " + TaskInstance.class.getName() + " ti where ti.processInstance.id in (:procInstIds) and ti.task.name = :name and ti.end is not null",
				Object[].class,
				new Param("procInstIds", procInstIds),
				new Param("name", taskName)
		);
		if (ListUtil.isEmpty(allData)) {
			return null;
		}

		Map<Long, List<Long>> results = new HashMap<>();
		for (Object[] data: allData) {
			if (ArrayUtil.isEmpty(data) || data.length < 2) {
				continue;
			}

			Object tmp = data[0];
			Long procInstId = null;
			if (tmp instanceof Number) {
				procInstId = ((Number) tmp).longValue();
			}
			if (procInstId == null) {
				continue;
			}

			List<Long> tiIds = results.get(procInstId);
			if (tiIds == null) {
				tiIds = new ArrayList<>();
				results.put(procInstId, tiIds);
			}

			tmp = data[1];
			Long tiId = null;
			if (tmp instanceof Number) {
				tiId = ((Number) tmp).longValue();
			}
			if (tiId != null) {
				tiIds.add(tiId);
			}
		}
		return results;
	}

	/*
	 * (non-Javadoc)
	 * @see com.idega.jbpm.data.dao.BPMDAO#findAllVariableBytesById(java.lang.Long)
	 */
	@Override
	public List<VariableBytes> findAllVariableBytesById(Long id) {
		if (id != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * FROM JBPM_BYTEBLOCK jb ")
			.append("WHERE jb.BYTES_ IS NOT NULL ")
			.append("AND jb.PROCESSFILE_ = ")
			.append(id);

			List<Serializable[]> rows = null;
			try {
				rows = SimpleQuerier.executeQuery(sb.toString(), 3);
			} catch (Exception e) {
				getLogger().log(Level.WARNING,
						"Failed to get VariableBytes by id: '" + id +
						"' cause of: ", e);
			}

			if (!ListUtil.isEmpty(rows)) {
				List<VariableBytes> result = new ArrayList<VariableBytes>(rows.size());
				for (Serializable[] row : rows) {
					if (!(row[0] instanceof BigDecimal) || !(row[2] instanceof BigDecimal)) {
						continue;
					}

					byte[] bytes = (byte[]) row[1];
					if (bytes == null || bytes.length < 1) {
						continue;
					}

					Long processFile = ((BigDecimal) row[0]).longValue();
					Integer index = ((BigDecimal) row[2]).intValue();

					result.add(new VariableBytes(processFile, bytes, index));
				}

				return result;
			}
		}

		return Collections.emptyList();
	}

	@Override
	public List<Long> getTaskInstancesIdsByTokenId(Long tokenId) {
		if (tokenId == null) {
			return null;
		}

		return getResultListByInlineQuery("select ti.id from " + TaskInstance.class.getName() + " ti where ti.token.id = :tokenId", Long.class, new Param("tokenId", tokenId));
	}
}