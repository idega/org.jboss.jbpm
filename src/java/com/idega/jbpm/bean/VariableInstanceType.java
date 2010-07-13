package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.List;

public enum VariableInstanceType implements Serializable {

	STRING {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.STRING_TYPES;
		}

		@Override
		public String getPrefix() {
			return "string_";
		}
	},
	
	BYTE_ARRAY {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.BYTE_ARRAY_TYPES;
		}
		
		@Override
		public String getPrefix() {
			return "files_";
		}
	},
	
	DATE {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.DATE_TYPES;
		}
		
		@Override
		public String getPrefix() {
			return "date_";
		}
	},
	
	DOUBLE {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.DOUBLE_TYPES;
		}
		
		@Override
		public String getPrefix() {
			return "double_";
		}
	},
	
	LONG {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.LONG_TYPES;
		}
		
		@Override
		public String getPrefix() {
			return "null_";
		}
	},
	
	NULL {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.NULL_TYPES;
		}
		
		@Override
		public String getPrefix() {
			return "null_";
		}
	},
	
	JCR_NODE {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.JCR_NODE_TYPES;
		}
		
		@Override
		public String getPrefix() {
			return "node_";
		}
	};
	
	public abstract List<String> getTypeKeys();
	public abstract String getPrefix();
}