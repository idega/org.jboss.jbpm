package com.idega.jbpm.process.business;

import javax.faces.context.FacesContext;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

import com.idega.presentation.IWContext;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.11 $
 *
 * Last modified: $Date: 2008/05/19 15:46:44 $ by $Author: civilis $
 */
public class SendParticipantInvitationMessageHandler implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;
	
	public SendParticipantInvitationMessageHandler() { }
	
	public SendParticipantInvitationMessageHandler(String parm) { }
	
	public void execute(ExecutionContext ctx) throws Exception {
		
		FacesContext fctx = FacesContext.getCurrentInstance();
		IWContext iwc = IWContext.getIWContext(fctx);
		SendParticipantInvitationMessageHandlerBean bean = (SendParticipantInvitationMessageHandlerBean)WFUtil.getBeanInstance(iwc, SendParticipantInvitationMessageHandlerBean.beanIdentifier);
		bean.send(ctx);
	}
}