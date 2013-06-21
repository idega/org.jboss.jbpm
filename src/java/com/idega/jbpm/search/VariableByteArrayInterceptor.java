package com.idega.jbpm.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import com.idega.jbpm.data.VariableByteArray;

public class VariableByteArrayInterceptor implements EntityIndexingInterceptor<VariableByteArray> {

	@Override
	public IndexingOverride onAdd(VariableByteArray entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onUpdate(VariableByteArray entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onDelete(VariableByteArray entity) {
		return IndexingOverride.REMOVE;
	}

	@Override
	public IndexingOverride onCollectionUpdate(VariableByteArray entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

}