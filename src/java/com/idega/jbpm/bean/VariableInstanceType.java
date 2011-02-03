package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
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
			return "long_";
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
	},
	
	LIST {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.BYTE_ARRAY_TYPES;
		}

		@Override
		public String getPrefix() {
			return "list_";
		}	
	},
	
	OBJ_LIST {
		@Override
		public List<String> getTypeKeys() {
			return BPMProcessVariable.BYTE_ARRAY_TYPES;
		}

		@Override
		public String getPrefix() {
			return "objlist_";
		}
	};
	
	public abstract List<String> getTypeKeys();
	public abstract String getPrefix();
	
	public static final List<VariableInstanceType> ALL_TYPES = Collections.unmodifiableList(Arrays.asList(
			VariableInstanceType.STRING,
			VariableInstanceType.BYTE_ARRAY,
			VariableInstanceType.DATE,
			VariableInstanceType.DOUBLE,
			VariableInstanceType.LONG,
			VariableInstanceType.NULL,
			VariableInstanceType.JCR_NODE,
			VariableInstanceType.LIST,
			VariableInstanceType.OBJ_LIST
	));
	
	public static final List<String> getVariableTypeKeys(String variableName) {
		for (VariableInstanceType type: ALL_TYPES) {
			if (variableName.startsWith(type.getPrefix())) {
				return type.getTypeKeys();
			}
		}
		return null;
	}
}