package com.idega.jbpm.data.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.data.Remove_ProcessViewByActor;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/28 07:43:40 $ by $Author: civilis $
 */
public interface Remove_ProcessViewDao extends GenericDao {

	public abstract Remove_ProcessViewByActor getProcessViewByViewerType(String viewerType, String viewType, Long processDefinition);
	
	public abstract List<Remove_ProcessViewByActor> getByViewType(String viewType, Long processDefinition);
}