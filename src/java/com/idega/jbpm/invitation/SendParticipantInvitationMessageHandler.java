package com.idega.jbpm.invitation;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.EmailValidator;
import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import com.idega.business.IBORuntimeException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.UserPersonalData;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.SendMail;
import com.idega.util.URIUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/06/15 11:58:32 $ by $Author: civilis $
 */
public class SendParticipantInvitationMessageHandler implements ActionHandler {

	private static final long serialVersionUID = -4337747330253308754L;
	
	private static final String defaultAssetsViewPageType = "bpm_assets_view";
	public static final String processInstanceIdParam = "piId";
	
	private String processInstanceIdExp;
	private String bpmUserIdExp;
	private String userDataExp;
	private String messageExp;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		if(getProcessInstanceIdExp() != null && getUserDataExp() != null && getBpmUserIdExp() != null) {
			
			final Long pid = 						(Long)JbpmExpressionEvaluator.evaluate(getProcessInstanceIdExp(), ectx);
			final Integer bpmUserId = 				(Integer)JbpmExpressionEvaluator.evaluate(getBpmUserIdExp(), ectx);
			final ProcessInstance pi = ectx.getJbpmContext().getProcessInstance(pid);
			final UserPersonalData upd = 			(UserPersonalData)JbpmExpressionEvaluator.evaluate(getUserDataExp(), ectx);
			final Message msg = 					getMessageExp() != null ? (Message)JbpmExpressionEvaluator.evaluate(getMessageExp(), ectx) : null;
			
//			SendParticipantInvitationMessageHandlerBean bean = ELUtil.getInstance().getBean(SendParticipantInvitationMessageHandlerBean.beanIdentifier);
//			bean.send(msg, upd, role, pi, ectx);
			
			final IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
			final IWResourceBundle iwrb = getResourceBundle(iwc);
			
			String recepientEmail = upd.getUserEmail();
			
			if(recepientEmail == null || !EmailValidator.getInstance().isValid(recepientEmail)) {
				
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Participant email address provided is not valid: "+recepientEmail);
				return;
			}
			
//			TODO: think about language choice
			
			String subject = msg != null ? msg.getSubject() : null;
			String text = msg != null ? msg.getText() : null;
			String from = msg != null ? msg.getFrom() : null;
			
			if(subject == null || CoreConstants.EMPTY.equals(subject)) {
				subject = iwrb.getLocalizedString("cases_bpm.case_invitation", "You've been invited to participate in case");
			}
			
			if(text == null) {
				text = CoreConstants.EMPTY;
			}
			
			if(from == null || CoreConstants.EMPTY.equals(from) || !EmailValidator.getInstance().isValid(from)) {
				from = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, "staff@idega.is");
			}
			
			String host = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, "mail.idega.is");
			
			
			//String fullUrl = getBuilderService(iwc).getFullPageUrlByPageType(iwc, egovBPMPageType);
			String fullUrl = getAssetsUrl(iwc);
			
			final URIUtil uriUtil = new URIUtil(fullUrl);
			
			uriUtil.setParameter(processInstanceIdParam, String.valueOf(pi.getId()));
			//uriUtil.setParameter(tokenParam, String.valueOf(tokenId));
			uriUtil.setParameter(BPMUser.bpmUsrParam, bpmUserId.toString());
			fullUrl = uriUtil.getUri();
			
//			System.out.println("fullUrl="+fullUrl);
			
//			String fullUrl = composeFullUrl(iwc, ctx.getToken());
			
			text += "\n" + iwrb.getLocalizedAndFormattedString("cases_bpm.case_invitation_message", "Follow the link to register and participate in the case : {0}", new Object[] {fullUrl}) ;
			
			try {
				SendMail.send(from, recepientEmail, null, null, host, subject, text);
			} catch (javax.mail.MessagingException me) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while sending participant invitation message", me);
			}
		}
	}
	
	private String getAssetsUrl(IWContext iwc) {
		
//		TODO: try to resolve url from app prop, if fail, then use default page type, and resolve from it (as it is now)
		String fullUrl = getBuilderService(iwc).getFullPageUrlByPageType(iwc, defaultAssetsViewPageType);
		return fullUrl;
	}
	
	protected BuilderService getBuilderService(IWApplicationContext iwc) {
		try {
			return BuilderServiceFactory.getBuilderService(iwc);
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		}
	}
	
	protected IWResourceBundle getResourceBundle(IWContext iwc) {
		IWMainApplication app = iwc.getIWMainApplication();
		IWBundle bundle = app.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		if(bundle != null) {
			return bundle.getResourceBundle(iwc);
		} else {
			return null;
		}
	}

	public String getUserDataExp() {
		return userDataExp;
	}

	public void setUserDataExp(String userDataExp) {
		this.userDataExp = userDataExp;
	}

	public String getMessageExp() {
		return messageExp;
	}

	public void setMessageExp(String messageExp) {
		this.messageExp = messageExp;
	}

	public String getProcessInstanceIdExp() {
		return processInstanceIdExp;
	}

	public void setProcessInstanceIdExp(String processInstanceIdExp) {
		this.processInstanceIdExp = processInstanceIdExp;
	}

	public String getBpmUserIdExp() {
		return bpmUserIdExp;
	}

	public void setBpmUserIdExp(String bpmUserIdExp) {
		this.bpmUserIdExp = bpmUserIdExp;
	}
}