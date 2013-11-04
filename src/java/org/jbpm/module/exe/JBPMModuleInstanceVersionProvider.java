package org.jbpm.module.exe;

import java.io.Serializable;

import com.idega.jbpm.version.BPMInstanceVersionProvider;

public abstract class JBPMModuleInstanceVersionProvider<I extends Serializable> implements BPMInstanceVersionProvider<I> {

	@Override
	public Number getVersion(I instance) {
		if (instance instanceof ModuleInstance) {
			ModuleInstance moduleInstance = (ModuleInstance) instance;
			return moduleInstance.version;
		}
		return null;
	}

}