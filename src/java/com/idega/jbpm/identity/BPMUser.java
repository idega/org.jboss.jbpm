package com.idega.jbpm.identity;

import java.util.List;

import com.idega.user.data.User;
import com.idega.util.URIParam;

/**
 * Wrapper of at least one, or two user entities, which correspond to bpm-user account and-if to
 * logged-in user's account. <br />
 * bpm-user account represents shared account, which unifies not logged in users, and/or logged in
 * users. <br />
 * Use case example is when user gets to the process by following link.
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.10 $ Last modified: $Date: 2009/01/22 11:12:35 $ by $Author: civilis $
 */
public interface BPMUser {
	
	public static final String defaultAssetsViewPageType = "bpm_assets_view";
	public static final String processInstanceIdParam = "piId";
	
	public static final String bpmUsrParam = "bpmusr";
	public static final String USER_TYPE_NATURAL = "BPM_USER_NATURAL";
	public static final String USER_TYPE_LEGAL = "BPM_USER_LEGAL";
	public static final String USER_TYPE = "BPM_USER_TYPE";
	public static final String USER_ROLE = "BPM_USER_ROLE";
	public static final String PROCESS_INSTANCE_ID = "BPM_PROCESS_INSTANCE_ID";
	public static final String HIDE_IN_CONTACTS = "BPM_HIDE_IN_CONTACTS";
	
	public abstract User getBpmUser();
	
	public abstract User getRealUser();
	
	public abstract Boolean getIsAssociated();
	
	public abstract Boolean getIsAssociated(boolean clean);
	
	public abstract Integer getIdToUse();
	
	public abstract String getUrlToTheProcess();
	
	public abstract User getUserToUse();
	
	/**
	 * @return the params for the link to access bpm user (and put it to session for assignment with
	 *         logged in user)
	 */
	public abstract List<URIParam> getParamsForBPMUserLink();
}