package com.idega.bpm.model;

import java.io.Serializable;

import com.idega.jbpm.bean.VariableInstanceType;

public interface VariableInstance extends Serializable {

	public <T extends Serializable> T getVariableId();

	public String getName();

	public <T extends Serializable> T getVariableValue();

	public Object getRawValue();

	public <T extends Serializable> void setVariableValue(T value);

	public <T extends Serializable> T getProcessInstanceId();

	public <T extends Serializable> T getTaskInstanceId();

	public VariableInstanceType getTypeOfVariable();

}