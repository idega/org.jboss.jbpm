package com.idega.jbpm.process.business;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.EmailValidator;
import org.jboss.jbpm.IWBundleStarter;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBORuntimeException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.jbpm.identity.BPMUser;
import com.idega.jbpm.identity.BPMUserFactory;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.SendMail;
import com.idega.util.URIUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/19 15:46:45 $ by $Author: civilis $
 */
@Scope("singleton")
@Service(SendParticipantInvitationMessageHandlerBean.beanIdentifier)
public class SendParticipantInvitationMessageHandlerBean {

	public static final String beanIdentifier = "jbpm_SendParticipantInvitationMessageHandlerBean";
	public static final String participantEmailVarName = "string:participantEmail";
	public static final String messageVarName = "string:message";
	public static final String subjectVarName = "string:subject";
	public static final String fromEmailVarName = "string:fromEmail";
	private static final String egovBPMPageType = "bpm_registerProcessParticipant";
	public static final String tokenParam = "bpmtkn";
	public static final String bpmUserParam = "bpmusr";
	
	private BPMUserFactory bpmUserFactory;
	
	public void send(ExecutionContext ctx) {
		
		long tokenId = ctx.getToken().getId();
		BPMUser bpmUser = getBpmUserFactory().createBPMUser(String.valueOf(tokenId));
		
		final IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		String recepientEmail = (String)ctx.getVariable(participantEmailVarName);
		
		if(recepientEmail == null || !EmailValidator.getInstance().isValid(recepientEmail)) {
			
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Participant email address provided is not valid: "+recepientEmail);
			return;
		}
		
//		TODO: think about language choice
		String subject = (String)ctx.getVariable(subjectVarName);
		String message = (String)ctx.getVariable(messageVarName);
		String from = (String)ctx.getVariable(fromEmailVarName);
		
		if(subject == null || CoreConstants.EMPTY.equals(subject)) {
			subject = iwrb.getLocalizedString("case_bpm.case_invitation", "You've been invited to participate in case");
		}
		
		if(message == null) {
			message = CoreConstants.EMPTY;
		}
		
		if(from == null || CoreConstants.EMPTY.equals(from) || !EmailValidator.getInstance().isValid(from)) {
			from = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, "staff@idega.is");
		}
		
		String host = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, "mail.idega.is");
		
		String fullUrl = getBuilderService(iwc).getFullPageUrlByPageType(iwc, egovBPMPageType);
		
		final URIUtil uriUtil = new URIUtil(fullUrl);
		uriUtil.setParameter(tokenParam, String.valueOf(tokenId));
		uriUtil.setParameter(bpmUserParam, String.valueOf(bpmUser.getBpmUser().getPrimaryKey().toString()));
		fullUrl = uriUtil.getUri();
		
//		String fullUrl = composeFullUrl(iwc, ctx.getToken());
		
		message += "\n" + iwrb.getLocalizedString("case_bpm.case_invitation_message", "Follow the link to register and participate in the case") + ":" + fullUrl;
		
		try {
			SendMail.send(from, recepientEmail, null, null, host, subject, message);
		} catch (javax.mail.MessagingException me) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while sending participant invitation message", me);
		}
	}
	
	/*
	protected String composeFullUrl(IWContext iwc, Token token) {
		
		String serverURL = iwc.getServerURL();
		String pageUri = getPageUri(iwc);
		
		final URIUtil uriUtil = new URIUtil(pageUri);
		uriUtil.setParameter(tokenParam, String.valueOf(token.getId()));
		pageUri = uriUtil.getUri();
		
		serverURL = serverURL.endsWith(CoreConstants.SLASH) ? serverURL.substring(0, serverURL.length()-1) : serverURL;
		
		String fullURL = new StringBuilder(serverURL)
		.append(pageUri.startsWith(CoreConstants.SLASH) ? CoreConstants.EMPTY : CoreConstants.SLASH)
		.append(pageUri)
		.toString();
		
		return fullURL;
	}
	
	protected String getPageUri(IWApplicationContext iwac) {
		
		Collection<ICPage> icpages = getPages(egovBPMPageType);
		
		ICPage icPage = null;
		
		if(icpages == null || icpages.isEmpty()) {
			
//			TODO: create egov bpm page, as not found
			throw new RuntimeException("No page found by page type: "+egovBPMPageType);			
		}
		
		if(icPage == null)
			icPage = icpages.iterator().next();
		
		String uri = icPage.getDefaultPageURI();
		
		if(!uri.startsWith("/pages"))
			uri = "/pages"+uri;
		
		return iwac.getIWMainApplication().getTranslatedURIWithContext(uri);
	}
	
	protected Collection<ICPage> getPages(String pageSubType) {
		
		try {
		
			ICPageHome home = (ICPageHome) IDOLookup.getHome(ICPage.class);
			@SuppressWarnings("unchecked")
			Collection<ICPage> icpages = home.findBySubType(pageSubType, false);
			
			return icpages;
			
		} catch (Exception e) {
			throw new RuntimeException("Exception while resolving icpages by subType: "+pageSubType, e);
		}
	}
	*/
	
	protected IWResourceBundle getResourceBundle(IWContext iwc) {
		IWMainApplication app = iwc.getIWMainApplication();
		IWBundle bundle = app.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		if(bundle != null) {
			return bundle.getResourceBundle(iwc);
		} else {
			return null;
		}
	}
	
	protected BuilderService getBuilderService(IWApplicationContext iwc) {
		try {
			return BuilderServiceFactory.getBuilderService(iwc);
			
		} catch (RemoteException e) {
			throw new IBORuntimeException(e);
		}
	}

	public BPMUserFactory getBpmUserFactory() {
		return bpmUserFactory;
	}

	@Autowired
	public void setBpmUserFactory(BPMUserFactory bpmUserFactory) {
		this.bpmUserFactory = bpmUserFactory;
	}
}