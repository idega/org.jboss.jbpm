package com.idega.jbpm.version;

import java.io.Serializable;

public interface BPMInstanceVersionProvider<I extends Serializable> {

	public Number getVersion(I instance);

}