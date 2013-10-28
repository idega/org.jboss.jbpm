package com.idega.jbpm.data.dao;

import com.idega.util.ArrayUtil;

public enum BPMVariableEnum {

	VariableInstance (org.jbpm.context.exe.VariableInstance.class.getName()),
	ByteArrayInstance (org.jbpm.context.exe.variableinstance.ByteArrayInstance.class.getName()),
	DateInstance (org.jbpm.context.exe.variableinstance.DateInstance.class.getName()),
	DoubleInstance (org.jbpm.context.exe.variableinstance.DoubleInstance.class.getName()),
//	Ejb3Instance (org.jbpm.context.exe.variableinstance.Ejb3Instance.class.getName()),
	HibernateLongInstance (org.jbpm.context.exe.variableinstance.HibernateLongInstance.class.getName()),
	HibernateStringInstance (org.jbpm.context.exe.variableinstance.HibernateStringInstance.class.getName()),
//	JcrNodeInstance (org.jbpm.context.exe.variableinstance.JcrNodeInstance.class.getName()),
	LongInstance (org.jbpm.context.exe.variableinstance.LongInstance.class.getName()),
//	NullInstance (org.jbpm.context.exe.variableinstance.NullInstance.class.getName()),
	StringInstance (org.jbpm.context.exe.variableinstance.StringInstance.class.getName());
//	UnpersistableInstance (org.jbpm.context.exe.variableinstance.UnpersistableInstance.class.getName());

	private String entityClass;

	private BPMVariableEnum(String entityClass) {
		this.entityClass = entityClass;
	}

	public String getEntityClass() {
		return entityClass;
	}

	public static BPMVariableEnum getEnum(String entityClass) {
		BPMVariableEnum[] enums = values();
		if (ArrayUtil.isEmpty(enums))
			return null;

		for (BPMVariableEnum varEnum: enums) {
			if (entityClass.equals(varEnum.getEntityClass()))
				return varEnum;
		}

		return null;
	}

}