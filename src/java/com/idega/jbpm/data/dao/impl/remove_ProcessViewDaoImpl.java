package com.idega.jbpm.data.dao.impl;

import java.util.List;

import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.jbpm.data.Remove_ProcessViewByActor;
import com.idega.jbpm.data.dao.Remove_ProcessViewDao;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/28 07:43:40 $ by $Author: civilis $
 */
public class remove_ProcessViewDaoImpl extends GenericDaoImpl implements Remove_ProcessViewDao {

	public Remove_ProcessViewByActor getProcessViewByViewerType(String viewerType, String viewType, Long processDefinition) {
		
		return (Remove_ProcessViewByActor)getEntityManager().createNamedQuery(Remove_ProcessViewByActor.GET_BY_VIEWER_TYPE_QUERY_NAME)
		.setParameter(Remove_ProcessViewByActor.viewerTypeParam, viewerType)
		.setParameter(Remove_ProcessViewByActor.viewTypeParam, viewType)
		.setParameter(Remove_ProcessViewByActor.processDefinitionIdParam, processDefinition)
		.getSingleResult();
	}
	
	public List<Remove_ProcessViewByActor> getByViewType(String viewType, Long processDefinition) {
		
		@SuppressWarnings("unchecked")
		List<Remove_ProcessViewByActor> views = getEntityManager().createNamedQuery(Remove_ProcessViewByActor.GET_BY_VIEW_TYPE_QUERY_NAME)
		.setParameter(Remove_ProcessViewByActor.viewTypeParam, viewType)
		.setParameter(Remove_ProcessViewByActor.processDefinitionIdParam, processDefinition)
		.getResultList();

		return views;
	}
}