package com.idega.jbpm.version;

import java.io.Serializable;

public interface BPMInstanceModificationProvider<I extends Serializable> {

	public Number getVersion(I instance);

	public void setId(I instance, Long id);

}