package org.jbpm.module.exe;

import java.io.Serializable;

import com.idega.jbpm.version.BPMInstanceModificationProvider;

public abstract class JBPMModuleInstanceVersionProvider<I extends Serializable> implements BPMInstanceModificationProvider<I> {

	@Override
	public Number getVersion(I instance) {
		if (instance instanceof ModuleInstance) {
			ModuleInstance moduleInstance = (ModuleInstance) instance;
			return moduleInstance.version;
		}
		return null;
	}

	@Override
	public void setId(I instance, Long id) {
		if (instance instanceof ModuleInstance) {
			((ModuleInstance) instance).id = id;
		}
	}

}