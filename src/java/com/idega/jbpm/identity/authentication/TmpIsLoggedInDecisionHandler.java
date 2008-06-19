package com.idega.jbpm.identity.authentication;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.BPMUser;
import com.idega.util.expression.ELUtil;

/**
 * Tmp jbpm action handler, checks if current bpm user is null. Expression should be used in definition
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/06/19 07:52:02 $ by $Author: civilis $
 */
public class TmpIsLoggedInDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = 1952352463607497379L;
	private static final String booleanTrue = 	"true";
	private static final String booleanFalse = 	"false";
	private BPMFactory bpmFactory;
	
	public String decide(ExecutionContext ectx) throws Exception {
		
//		injecting spring dependencies, as this is not yet spring managed bean
//		TODO: remove this when moved to spring (or seam) bean
		ELUtil.getInstance().autowire(this);
		
		BPMUser bpmUser = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();

		return bpmUser.getBpmUser() != null ? booleanTrue : booleanFalse;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	@Autowired
	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}