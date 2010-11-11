package com.idega.jbpm.bean;

import java.util.Comparator;

public class VariableInstanceInfoComparator implements Comparator<VariableInstanceInfo> {

	@Override
	public int compare(VariableInstanceInfo var1, VariableInstanceInfo var2) {
		if (var1 == null || var2 == null) {
			return 0;
		}
		
		Long id1 = var1.getId();
		if (id1 == null) {
			return 0;
		}
		Long id2 = var2.getId();
		if (id2 == null) {
			return 0;
		}
		
		return -(id1.compareTo(id2));
	}

}