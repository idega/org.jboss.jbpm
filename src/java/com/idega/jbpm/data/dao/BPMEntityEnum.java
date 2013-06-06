package com.idega.jbpm.data.dao;

import com.idega.util.ArrayUtil;

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

	};

	private String entityClass;

	private BPMEntityEnum(String entityClass) {
		this.entityClass = entityClass;
	}

	public String getEntityClass() {
		return entityClass;
	}

	public abstract String getVersionQuery(long id);
	public abstract String getUpdateQuery(long id, int version);

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