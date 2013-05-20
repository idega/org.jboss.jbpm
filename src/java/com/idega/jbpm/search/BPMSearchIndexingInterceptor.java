package com.idega.jbpm.search;

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;

import com.idega.jbpm.data.Variable;

public class BPMSearchIndexingInterceptor implements EntityIndexingInterceptor<Variable> {

	@Override
	public IndexingOverride onAdd(Variable entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onUpdate(Variable entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	@Override
	public IndexingOverride onDelete(Variable entity) {
		return IndexingOverride.REMOVE;
	}

	@Override
	public IndexingOverride onCollectionUpdate(Variable entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

}