package org.jboss.jbpm;

import java.util.Iterator;

import org.hibernate.ejb.event.EJB3FlushEntityEventListener;
import org.hibernate.ejb.event.EJB3FlushEventListener;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.persistence.IdegaFlushEntityEventListener;
import com.idega.jbpm.persistence.IdegaFlushEventListener;
import com.idega.jbpm.search.BPMSearchIndex;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/16 09:47:41 $ by $Author: civilis $
 */
public class IWBundleStarter implements IWBundleStartable {

	public static final String IW_BUNDLE_IDENTIFIER = "org.jboss.jbpm";

	@Autowired
	private BPMDAO bpmDAO;

	@Autowired
	private VariableInstanceQuerier querier;

	@Autowired
	private BPMSearchIndex searchIndex;

	private BPMSearchIndex getSearchIndexForVariable() {
		if (searchIndex == null)
			ELUtil.getInstance().autowire(this);
		return searchIndex;
	}

	private <T> void doManageFlushListener(EventListenerGroup<T> listenersGroup, String classNameToRemove, T listenerToAdd) {
		boolean hasListener = false;
		for (Iterator<T> iter = listenersGroup.listeners().iterator(); iter.hasNext();) {
			T listener = iter.next();
			String name = listener.getClass().getName();
			if (name.equals(classNameToRemove)) {
				iter.remove();
			} else if (name.equals(listenerToAdd.getClass().getName())) {
				hasListener = true;
			}
		}
		if (!hasListener) {
			listenersGroup.prependListener(listenerToAdd);
		}
	}

	@Override
	public void start(IWBundle starterBundle) {
		Object session = getVariableInstanceQuerier().getEntityManager().getDelegate();
		if (session instanceof SessionImpl) {
			EventListenerRegistry eventListenerRegistry = ((SessionImpl) session)
					.getFactory()
					.getServiceRegistry()
					.getService(EventListenerRegistry.class);

			FlushEntityEventListener flushEntityEventListener = new IdegaFlushEntityEventListener();
			EventListenerGroup<FlushEntityEventListener> flushEntityEventListenersGroup = eventListenerRegistry
						.getEventListenerGroup(EventType.FLUSH_ENTITY);
			doManageFlushListener(flushEntityEventListenersGroup, EJB3FlushEntityEventListener.class.getName(), flushEntityEventListener);

			FlushEventListener flushEventListener = new IdegaFlushEventListener();
			EventListenerGroup<FlushEventListener> flushEventListenersGroup = eventListenerRegistry
					.getEventListenerGroup(EventType.FLUSH);
			doManageFlushListener(flushEventListenersGroup, EJB3FlushEventListener.class.getName(), flushEventListener);
		}

		doBuildIndexes(starterBundle.getApplication().getSettings());

		BPMViewManager vm = BPMViewManager.getInstance(starterBundle.getApplication());
		vm.initializeStandardNodes(starterBundle);

		Thread variablesDataImporter = new Thread(new Runnable() {
			@Override
			public void run() {
				getBpmDAO().importVariablesData();
			}
		});
		variablesDataImporter.start();

		Thread procVarsBinder = new Thread(new Runnable() {
			@Override
			public void run() {
				getVariableInstanceQuerier().doBindProcessVariables();
			}
		});
		procVarsBinder.start();
	}

	private void doBuildIndexes(IWMainApplicationSettings settings) {
		try {
			if (settings.getBoolean("bpm.auto_index_vars", Boolean.FALSE)) {
				getSearchIndexForVariable().rebuild();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

	BPMDAO getBpmDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmDAO;
	}

	@Override
	public void stop(IWBundle starterBundle) { }
}