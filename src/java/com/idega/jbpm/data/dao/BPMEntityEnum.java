package com.idega.jbpm.data.dao;

import java.io.Serializable;
import java.util.List;

import com.idega.data.SimpleQuerier;
import com.idega.jbpm.version.BPMInstanceVersionProvider;
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
			return getVersionUpdater(getEntityType()).getVersion((org.jbpm.graph.exe.Token) entity);
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
			return getVersionUpdater(getEntityType()).getVersion((org.jbpm.taskmgmt.exe.TaskInstance) entity);
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
			return getVersionUpdater(getEntityType()).getVersion((org.jbpm.taskmgmt.exe.TaskMgmtInstance) entity);
		}
	},

	TokenVariableMap (org.jbpm.context.exe.TokenVariableMap.class.getName()) {

		@Override
		public Number getEntityVersion(Object entity) {
			return getVersionUpdater(getEntityType()).getVersion((org.jbpm.context.exe.TokenVariableMap) entity);
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

	};

	private String entityClass;

	private BPMEntityEnum(String entityClass) {
		this.entityClass = entityClass;
	}

	private static <T extends Serializable> BPMInstanceVersionProvider<T> getVersionUpdater(Class<T> type) {
		try {
			BPMInstanceVersionProvider<T> updater = ELUtil.getInstance().getBean("jbpm" + type.getSimpleName() + "VersionUpdater");
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