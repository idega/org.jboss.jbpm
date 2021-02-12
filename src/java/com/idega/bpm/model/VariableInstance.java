package com.idega.bpm.model;

import java.io.Serializable;

public interface VariableInstance extends Serializable {

	public String getName();

	public <T extends Serializable> T getVariableValue();

	public <T extends Serializable> T getProcessInstanceId();

}