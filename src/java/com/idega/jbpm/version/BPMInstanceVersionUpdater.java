package com.idega.jbpm.version;

import java.io.Serializable;

import org.hibernate.Session;

public interface BPMInstanceVersionUpdater<I extends Serializable> {

	public boolean doUpdateInstanceVersion(I instance, int version);

	public boolean isPossibleToUpdateVersion(Session session, I instance);

	public Number getVersion(I instance);

}