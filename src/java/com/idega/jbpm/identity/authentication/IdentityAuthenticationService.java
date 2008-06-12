package com.idega.jbpm.identity.authentication;

import org.jbpm.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMUser;

/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.6 $
 * 
 * Last modified: $Date: 2008/06/12 18:29:46 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class IdentityAuthenticationService implements AuthenticationService {

	private static final long serialVersionUID = 356148645298265464L;
	private BPMFactory bpmFactory;

	public String getActorId() {

		BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
		
		if(bpmUser != null) {
		
			Integer usrId = bpmUser.getIdToUse();
			return usrId == null ? null : usrId.toString();
		}
		
		return null;
	}

	public void close() { }

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}