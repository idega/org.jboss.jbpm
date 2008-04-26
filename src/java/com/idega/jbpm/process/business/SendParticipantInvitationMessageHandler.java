package com.idega.jbpm.process.business;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.EmailValidator;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.SendMail;
import com.idega.util.URIUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/04/26 02:48:33 $ by $Author: civilis $
 */
public class SendParticipantInvitationMessageHandler implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;
	
	public static final String participantEmailVarName = "string:participantEmail";
	public static final String messageVarName = "string:message";
	public static final String subjectVarName = "string:subject";
	public static final String fromEmailVarName = "string:fromEmail";
	private static final String egovBPMPageType = "bpm_registerProcessParticipant";
	public static final String tokenParam = "bpmtkn";
	
	public SendParticipantInvitationMessageHandler() { }
	
	public SendParticipantInvitationMessageHandler(String parm) { }

	
	public void execute(ExecutionContext ctx) throws Exception {

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
			subject = "You've been invited to participate in case";
		}
		
		if(message == null) {
			message = CoreConstants.EMPTY;
		}
		
		final IWContext iwc = IWContext.getIWContext(FacesContext.getCurrentInstance());
		
		if(from == null || CoreConstants.EMPTY.equals(from) || !EmailValidator.getInstance().isValid(from)) {
			from = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_MAIL_FROM_ADDRESS, "staff@idega.is");
		}
		
		String host = iwc.getApplicationSettings().getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER, "mail.idega.is");
		
		String fullUrl = composeFullUrl(iwc, ctx.getToken());
		
		message += "\nFollow the link to register and participate in the case: "+fullUrl;
		
		SendMail.send(from, recepientEmail, null, null, host, subject, message);
	}
	
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
}