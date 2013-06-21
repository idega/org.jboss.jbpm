package com.idega.jbpm.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import com.idega.jbpm.data.VariableBytes;

public class VariableBytesInterceptor implements EntityIndexingInterceptor<VariableBytes> {

	@Override
	public IndexingOverride onAdd(VariableBytes entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onUpdate(VariableBytes entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onDelete(VariableBytes entity) {
		return IndexingOverride.REMOVE;
	}

	@Override
	public IndexingOverride onCollectionUpdate(VariableBytes entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

}