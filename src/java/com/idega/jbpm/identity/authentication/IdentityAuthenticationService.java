package com.idega.jbpm.identity.authentication;

import org.jbpm.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMUser;

@Scope("singleton")
@Service
public class IdentityAuthenticationService implements AuthenticationService {

	private static final long serialVersionUID = 356148645298265464L;
	private BPMFactory bpmFactory;

	public String getActorId() {

		BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
		Integer usrId = bpmUser.getIdToUse();
		
//		IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
//		String currentUserId = String.valueOf(iwc.getCurrentUserId());
		return usrId == null ? null : usrId.toString();
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