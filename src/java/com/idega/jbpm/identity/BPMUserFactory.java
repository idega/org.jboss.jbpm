package com.idega.jbpm.identity;

import com.idega.core.persistence.GenericDao;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.user.data.User;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2008/05/24 10:25:51 $ by $Author: civilis $
 */
public interface BPMUserFactory {

	public abstract BPMUser createBPMUser(String roleName,
			long processInstanceId);

	public abstract BPMUserImpl createUser();

	public abstract BPMUser getCurrentBPMUser();

	public abstract BPMUser getBPMUser(Integer bpmUserPK, User usr);

	public abstract boolean isAssociated(User realUsr, User bpmUsr,
			boolean autoAssociate);

	public abstract GenericDao getGenericDao();

	public abstract BPMFactory getBPMFactory();
}