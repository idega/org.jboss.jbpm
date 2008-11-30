package com.idega.jbpm.identity.authentication;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMUser;

/**
 * Checks if current bpm user contains real user - is logged in.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2008/11/30 08:17:52 $ by $Author: civilis $
 */
@Service("isCurrentUserLoggedInDecisionHandler")
@Scope("prototype")
public class IsLoggedInDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = 1952352463607497379L;
	private static final String booleanTrue = "true";
	private static final String booleanFalse = "false";
	@Autowired
	private BPMFactory bpmFactory;

	public String decide(ExecutionContext ectx) throws Exception {

		BPMUser bpmUser = getBpmFactory().getBpmUserFactory()
				.getCurrentBPMUser();
		return bpmUser.getRealUser() != null ? booleanTrue : booleanFalse;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}