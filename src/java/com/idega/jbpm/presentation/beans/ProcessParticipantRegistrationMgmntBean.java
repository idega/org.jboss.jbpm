package com.idega.jbpm.presentation.beans;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.builder.data.ICPage;
import com.idega.core.builder.data.ICPageHome;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.process.business.AssignAccountToParticipantHandler;
import com.idega.presentation.IWContext;
import com.idega.util.URIUtil;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/24 17:23:21 $ by $Author: civilis $
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(ProcessParticipantRegistrationMgmntBean.beanIdentifier)
public class ProcessParticipantRegistrationMgmntBean {
	
	public static final String beanIdentifier = "ProcessParticipantRegistrationMgmntBean";
	public static final String redirectURLVariable = "string:participantInvitationRedirectURL";
	public static final String egovBPMAssetsView = "egov_bpm_assetsView";
	
	
	private IdegaJbpmContext idegaJbpmContext;

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}

	public String getRedirectURL(long tokenId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Token token = ctx.getToken(tokenId);
			String redirectURL = (String)token.getProcessInstance().getContextInstance().getVariable(redirectURLVariable);
			
			if(redirectURL == null) {
				redirectURL = composeFullUrl(IWContext.getIWContext(FacesContext.getCurrentInstance()), token);
			}
			
			return redirectURL;
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
	
	protected String composeFullUrl(IWContext iwc, Token token) {
		
		String pageUri = getPageUri(iwc);
		
		if(pageUri == null)
			return null;
		
		URIUtil uriUtil = new URIUtil(pageUri);
		uriUtil.setParameter("piId", String.valueOf(token.getProcessInstance().getSuperProcessToken().getProcessInstance().getId()));
		pageUri = uriUtil.getUri();
		
		return pageUri;
	}
	
	protected String getPageUri(IWApplicationContext iwac) {
		
		Collection<ICPage> icpages = getPages(egovBPMAssetsView);
		
		ICPage icPage = null;
		
		if(icpages == null || icpages.isEmpty()) {
			
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No page found by page type: "+egovBPMAssetsView);
			return null;
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
	
	public void participantUserLoggedIn(long tokenId, int userId) {
		
		JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
		
		try {
			Token token = ctx.getToken(tokenId);
			token.getProcessInstance().getContextInstance().setVariable(AssignAccountToParticipantHandler.participantUserIdVarName, userId);
			token.signal();
			
		} finally {
			getIdegaJbpmContext().closeAndCommit(ctx);
		}
	}
}