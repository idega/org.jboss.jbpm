package com.idega.jbpm.presentation.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.login.presentation.Login2.LoginListener;
import com.idega.block.login.presentation.Register.RegisterListener;
import com.idega.jbpm.process.business.SendParticipantInivtationMessageHandler;
import com.idega.presentation.IWContext;
import com.idega.webface.WFUtil;


/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/04/03 13:37:19 $ by $Author: civilis $
 */
@Scope("request")
@Service("regProcessParticipantBean")
public class RegProcessParticipantBean implements Serializable, LoginListener, RegisterListener {

	private static final long serialVersionUID = -8356636747899018641L;
	
	private String processName;
	private Long tokenId;
	private String viewDisplayed;
	private boolean afterRegSuccess = false;
	
	public String getViewDisplayed() {
		return viewDisplayed;
	}
	public void setViewDisplayed(String viewDisplayed) {
		this.viewDisplayed = viewDisplayed;
	}
	public String getProcessName() {
		return processName;
	}
	public void setProcessName(String processName) {
		
		this.processName = processName;
	}
	
	public Long getTokenId() {
		
		if(tokenId == null) {
			String tokenIdStr = (String)FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(SendParticipantInivtationMessageHandler.tokenParam);
			
			if(tokenIdStr != null)
				tokenId = new Long(tokenIdStr);
		}
		
		return tokenId;
	}
	
	public void setTokenId(Long tokenId) {
		this.tokenId = tokenId;
	}
	public void loginFailed() {
	}
	public void loginSuccess() {
		
		try {
			if(getTokenId() == null) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "No token id");
				return;
			}
			
			FacesContext context = FacesContext.getCurrentInstance();
			ProcessParticipantRegistrationMgmntBean mgmtBean = getProcessParticipantRegistrationMgmntBean();
			mgmtBean.participantUserLoggedIn(getTokenId(), IWContext.getIWContext(context).getCurrentUserId());
			
			String redirectURL = mgmtBean.getRedirectURL(getTokenId());
			HttpServletResponse response = (HttpServletResponse)context.getExternalContext().getResponse();
			response.sendRedirect(redirectURL);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void registerSuccess() {
		setAfterRegSuccess(true);
	}
	
	public ProcessParticipantRegistrationMgmntBean getProcessParticipantRegistrationMgmntBean() {
		
		return (ProcessParticipantRegistrationMgmntBean)WFUtil.getBeanInstance(ProcessParticipantRegistrationMgmntBean.beanIdentifier);
	}
	public boolean isAfterRegSuccess() {
		return afterRegSuccess;
	}
	public void setAfterRegSuccess(boolean afterRegSuccess) {
		this.afterRegSuccess = afterRegSuccess;
	}
	public boolean isLoggedIn() {
		return IWContext.getIWContext(FacesContext.getCurrentInstance()).isLoggedOn();
	}
	public void setLoggedIn(boolean loggedIn) {
	}
}