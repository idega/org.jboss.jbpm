package com.idega.jbpm.data.dao;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;

import com.idega.data.SimpleQuerier;
import com.idega.jbpm.version.BPMInstanceModificationProvider;
import com.idega.util.ArrayUtil;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

public enum BPMEntityEnum {

	Token (org.jbpm.graph.exe.Token.class.getName()) {

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from jbpm_token where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update jbpm_token set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.graph.exe.Token.class;
		}

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.graph.exe.Token) entity);
		}

	},

	TaskInstance (org.jbpm.taskmgmt.exe.TaskInstance.class.getName()) {

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from jbpm_taskinstance where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update jbpm_taskinstance set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.taskmgmt.exe.TaskInstance.class;
		}

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.taskmgmt.exe.TaskInstance) entity);
		}

	},

	TaskMgmtInstance (org.jbpm.taskmgmt.exe.TaskMgmtInstance.class.getName()) {

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from JBPM_MODULEINSTANCE where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update JBPM_MODULEINSTANCE set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.taskmgmt.exe.TaskMgmtInstance.class;
		}

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.taskmgmt.exe.TaskMgmtInstance) entity);
		}
	},

	TokenVariableMap (org.jbpm.context.exe.TokenVariableMap.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.context.exe.TokenVariableMap) entity);
		}

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from JBPM_TOKENVARIABLEMAP where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update JBPM_TOKENVARIABLEMAP set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.context.exe.TokenVariableMap.class;
		}

	},

	ByteArrayInstance (org.jbpm.context.exe.variableinstance.ByteArrayInstance.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.context.exe.variableinstance.ByteArrayInstance) entity);
		}

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from jbpm_variableinstance where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update jbpm_variableinstance set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.context.exe.variableinstance.ByteArrayInstance.class;
		}

	},

	NullInstance (org.jbpm.context.exe.variableinstance.NullInstance.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.context.exe.variableinstance.NullInstance) entity);
		}

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from jbpm_variableinstance where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update jbpm_variableinstance set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.context.exe.variableinstance.NullInstance.class;
		}

	},

	StringInstance (org.jbpm.context.exe.variableinstance.StringInstance.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return getModificationProvider(getEntityType()).getVersion((org.jbpm.context.exe.variableinstance.StringInstance) entity);
		}

		@Override
		public String getVersionQuery(long id) {
			return "select version_ from jbpm_variableinstance where id_ = " + id;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return "update jbpm_variableinstance set version_ = " + version + " where id_ = " + id;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.context.exe.variableinstance.StringInstance.class;
		}

	},

	ByteArray (org.jbpm.bytes.ByteArray.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return null;
		}

		@Override
		public String getVersionQuery(long id) {
			return null;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.bytes.ByteArray.class;
		}

	},

	Event (org.jbpm.graph.def.Event.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return null;
		}

		@Override
		public String getVersionQuery(long id) {
			return null;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.graph.def.Event.class;
		}

	},

	Delegation (org.jbpm.instantiation.Delegation.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return null;
		}

		@Override
		public String getVersionQuery(long id) {
			return null;
		}

		@Override
		public String getUpdateQuery(long id, int version) {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends Serializable> Class<T> getEntityType() {
			return (Class<T>) org.jbpm.instantiation.Delegation.class;
		}

	};

	private String entityClass;

	private BPMEntityEnum(String entityClass) {
		this.entityClass = entityClass;
	}

	private static <T extends Serializable> BPMInstanceModificationProvider<T> getModificationProvider(Class<T> type) {
		try {
			BPMInstanceModificationProvider<T> updater = ELUtil.getInstance().getBean("jbpm" + type.getSimpleName() + "VersionUpdater");
			return updater;
		} catch (Exception e) {}
		return null;
	}

	public String getEntityClass() {
		return entityClass;
	}

	public abstract Number getEntityVersion(Object entity);

	public Number getVersionFromDatabase(Long id) {
		String versionQuery = getVersionQuery(id);
		if (versionQuery == null) {
			return null;
		}

		try {
			List<Serializable[]> results = SimpleQuerier.executeQuery(versionQuery, 1);
			if (!ListUtil.isEmpty(results)) {
				Serializable[] versions = results.iterator().next();
				if (!ArrayUtil.isEmpty(versions)) {
					Serializable version = versions[0];
					return Integer.valueOf(version.toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public <T extends Serializable> void setId(Long id, Session session, T object) {
		try {
			@SuppressWarnings("unchecked")
			Class<T> type = (Class<T>) object.getClass();
			BPMInstanceModificationProvider<T> updater = getModificationProvider(type);
			if (updater == null) {
				updater = getModificationProvider(getEntityType());
			}
			if (updater == null) {
				LOGGER.warning("Failed to find instance of " + BPMInstanceModificationProvider.class.getName() + " for type " + type.getName());
			} else {
				updater.setId(object, id);
				if (session.get(type, id) == null) {
					session.saveOrUpdate(object);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error setting ID (" + id + ") for " + object, e);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(BPMEntityEnum.class.getName());

	public abstract String getVersionQuery(long id);
	public abstract String getUpdateQuery(long id, int version);
	public abstract <T extends Serializable> Class<T> getEntityType();

	public static BPMEntityEnum getEnum(String entityClass) {
		BPMEntityEnum[] enums = values();
		if (ArrayUtil.isEmpty(enums))
			return null;

		for (BPMEntityEnum bpmEnum: enums) {
			if (entityClass.equals(bpmEnum.getEntityClass()))
				return bpmEnum;
		}

		return null;
	}
}