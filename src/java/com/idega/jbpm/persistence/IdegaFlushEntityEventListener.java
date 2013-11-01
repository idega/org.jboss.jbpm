package com.idega.jbpm.persistence;

import java.sql.SQLException;
import java.util.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.event.spi.FlushEntityEvent;

import com.idega.data.SimpleQuerier;
import com.idega.jbpm.data.dao.BPMEntityEnum;
import com.idega.util.StringHandler;

public class IdegaFlushEntityEventListener extends DefaultFlushEntityEventListener /*EJB3FlushEntityEventListener*/ {

	private static final long serialVersionUID = -3570810235084335673L;

	public IdegaFlushEntityEventListener() {
//		super(new EntityCallbackHandler());
	}

	public static final void doManageEntity(Object entity, EntityEntry entry) {
		if (entity == null || entry == null) {
			return;
		}

		String entryId = entry.getId().toString();
		if (StringHandler.isNumeric(entryId)) {
			BPMEntityEnum bpmEntity = BPMEntityEnum.getEnum(entity.getClass().getName());
			if (bpmEntity != null) {
				Long id = Long.valueOf(entryId);
				Number versionInDB = bpmEntity.getVersionFromDatabase(id);
				if (versionInDB instanceof Number) {
					Object previousVersion = entry.getVersion();
					if (previousVersion instanceof Number && versionInDB.intValue() != ((Number) previousVersion).intValue()) {
						String updateSQL = bpmEntity.getUpdateQuery(id, ((Number) previousVersion).intValue());
						try {
							SimpleQuerier.executeUpdate(updateSQL, false);
							Logger.getLogger(IdegaFlushEntityEventListener.class.getName())
								.info("Changed version to " + previousVersion + " for " + bpmEntity.getEntityClass() + ", ID: " + id +
										", class: " + bpmEntity.getEntityClass());
						} catch (SQLException e) {}
					}/* else {
						Logger.getLogger(IdegaFlushEntityEventListener.class.getName())
						.info("Keeping version " + previousVersion + " for " + bpmEntity.getEntityClass() + ", ID: " + entry.getId() +
								", class: " + bpmEntity.getEntityClass());
					}*/
				}
			}
		}
	}

	@Override
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
		Object entity = event.getEntity();
		doManageEntity(entity, event.getEntityEntry());

//		final Object entity = event.getEntity();
//		final EntityEntry entry = event.getEntityEntry();
//		final EventSource session = event.getSession();
//		final EntityPersister persister = entry.getPersister();
//		final Status status = entry.getStatus();
//		final Type[] types = persister.getPropertyTypes();
//
//		final boolean mightBeDirty = entry.requiresDirtyCheck(entity);

//		final Object[] values = getValues( entity, entry, mightBeDirty, session );
//
//		event.setPropertyValues(values);
//
//		//TODO: avoid this for non-new instances where mightBeDirty==false
//		boolean substitute = wrapCollections( session, persister, types, values);
//
//		if ( isUpdateNecessary( event, mightBeDirty ) ) {
//			substitute = scheduleUpdate( event ) || substitute;
//		}
//
//		if ( status != Status.DELETED ) {
//			// now update the object .. has to be outside the main if block above (because of collections)
//			if (substitute) persister.setPropertyValues( entity, values );
//
//			// Search for collections by reachability, updating their role.
//			// We don't want to touch collections reachable from a deleted object
//			if ( persister.hasCollections() ) {
//				new FlushVisitor(session, entity).processEntityPropertyValues(values, types);
//			}
//		}
		super.onFlushEntity(event);
	}

}