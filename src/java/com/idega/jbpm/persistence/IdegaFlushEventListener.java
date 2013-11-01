package com.idega.jbpm.persistence;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ejb.event.EJB3FlushEventListener;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.internal.util.collections.IdentityMap;

import com.idega.jbpm.data.dao.BPMEntityEnum;

public class IdegaFlushEventListener extends /*DefaultFlushEventListener*/EJB3FlushEventListener {

	private static final long serialVersionUID = 5445936877641058679L;

	@Override
	@SuppressWarnings("unchecked")
	public void onFlush(FlushEvent event) throws HibernateException {
		EventSource source = event.getSession();
		PersistenceContext persistenceContext = source.getPersistenceContext();
		for (Map.Entry<?, EntityEntry> entry : IdentityMap.concurrentEntries(persistenceContext.getEntityEntries())) {
			EntityEntry entityEntry = entry.getValue();
			BPMEntityEnum bpmEntity = BPMEntityEnum.getEnum(entityEntry.getEntityName());
			if (bpmEntity == null) {
				continue;
			}

			try {
				Long id = Long.valueOf(entityEntry.getId().toString());
				Object entity = source.get(Class.forName(bpmEntity.getEntityClass()), id);
				IdegaFlushEntityEventListener.doManageEntity(entity, entityEntry);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		super.onFlush(event);
	}

}