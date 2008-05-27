package com.idega.jbpm.identity;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 * 
 * Last modified: $Date: 2008/05/27 14:48:41 $ by $Author: civilis $
 */
public interface BPMUserFactory {

	public abstract User createBPMUser(String name, String roleName,
			long processInstanceId);

	public abstract BPMUserImpl createUser();

	public abstract BPMUser getCurrentBPMUser();

	public abstract BPMUser getBPMUser(Integer bpmUserPK, User usr);

	public abstract boolean isAssociated(User realUsr, User bpmUsr,
			boolean autoAssociate);

	public abstract GenericDao getGenericDao();

	public abstract BPMFactory getBPMFactory();
}