package com.idega.jbpm.search;

import java.lang.annotation.ElementType;

import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;

import com.idega.jbpm.data.Variable;
import com.idega.jbpm.data.VariableBytes;
import com.idega.jbpm.search.bridge.VariableBytesInstanceBridge;
import com.idega.jbpm.search.bridge.VariableDateInstanceBridge;

public class BPMSearchIndexMapping {

	@Factory
	public SearchMapping getSearchMapping() {
		SearchMapping mapping = new SearchMapping();
		mapping.entity(Variable.class)
			.indexed()
				.interceptor(VariableInterceptor.class)
					.property("id", ElementType.FIELD)
						.documentId()
					.property("name", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("processInstance", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("taskInstance", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("stringValue", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("longValue", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("doubleValue", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("dateValue", ElementType.FIELD)
						.bridge(VariableDateInstanceBridge.class)
						.field()
							.store(Store.YES)
					.property("classType", ElementType.FIELD)
						.field()
							.store(Store.YES)
					.property("bytesValue", ElementType.FIELD)
						.bridge(VariableBytesInstanceBridge.class)
						.field()
					.property("realObject", ElementType.FIELD)
						.bridge(VariableBytesInstanceBridge.class)
						.field();

//		mapping.entity(VariableByteArray.class)
//			.indexed()
//				.interceptor(VariableByteArrayInterceptor.class)
//					.property("bytes", ElementType.FIELD)
//						.bridge(VariableBytesInstanceBridge.class)
//						.indexEmbedded()
//						.field();

		mapping.entity(VariableBytes.class)
			.indexed()
				.interceptor(VariableBytesInterceptor.class)
					.property("bytes", ElementType.FIELD)
						.bridge(VariableBytesInstanceBridge.class)
						.field();

		return mapping;
	}

}
