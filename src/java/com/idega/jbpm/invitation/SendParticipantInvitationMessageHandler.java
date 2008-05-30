package com.idega.jbpm.invitation;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/30 15:10:39 $ by $Author: civilis $
 */
public class SendParticipantInvitationMessageHandler implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;
	
	public SendParticipantInvitationMessageHandler() { }
	
	public SendParticipantInvitationMessageHandler(String parm) { }
	
	public void execute(ExecutionContext ctx) throws Exception {
		
		SendParticipantInvitationMessageHandlerBean bean = (SendParticipantInvitationMessageHandlerBean)WFUtil.getBeanInstance(SendParticipantInvitationMessageHandlerBean.beanIdentifier);
		bean.send(ctx);
	}
}