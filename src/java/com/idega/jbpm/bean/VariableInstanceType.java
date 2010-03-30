package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.List;

public enum VariableInstanceType implements Serializable {

	STRING {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.STRING_TYPES;
		}
	},
	
	BYTE_ARRAY {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.BYTE_ARRAY_TYPES;
		}
	},
	
	DATE {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.DATE_TYPES;
		}
	},
	
	DOUBLE {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.DOUBLE_TYPES;
		}
	},
	
	LONG {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.LONG_TYPES;
		}
	},
	
	NULL {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.NULL_TYPES;
		}
	},
	
	JCR_NODE {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.JCR_NODE_TYPES;
		}
	};
	
	public abstract List<String> getTypeKeys();
}