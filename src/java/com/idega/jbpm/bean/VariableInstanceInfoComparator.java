package com.idega.jbpm.bean;

import java.io.Serializable;
import java.util.Comparator;

import com.idega.bpm.model.VariableInstance;

public class VariableInstanceInfoComparator implements Comparator<VariableInstance> {

	@Override
	public int compare(VariableInstance var1, VariableInstance var2) {
		if (var1 == null || var2 == null) {
			return 0;
		}

		Serializable id1 = var1.getVariableId();
		Serializable id2 = var2.getVariableId();

		if (id1 instanceof Number && id2 instanceof Number) {
			Long idLong1 = ((Number) id1).longValue();
			Long idLong2 = ((Number) id2).longValue();
			return -(idLong1.compareTo(idLong2));
		}

		return 0;
	}

}