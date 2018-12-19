package com.idega.jbpm.identity;

import com.idega.jbpm.data.NativeIdentityBind.IdentityType;

/**
 * Json expression representation to assign identity to role
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/02/23 12:38:50 $ by $Author: civilis $
 */
public class Identity {

	public Identity() {
	}

	public Identity(String identityId, IdentityType identityType) {
		setIdentityId(identityId);
		setIdentityType(identityType);
	}

	/**
	 * if used in the expression for identity type IdentityType.USER, means that the current logged
	 * in user needs to be assigned to the process actor
	 */
	public static final String currentUser = "current_user";

	private IdentityType identityType;
	/**
	 * used in json expression, to resolve identityId from expression. E.g. from expression
	 * current_user identityId of userId of current logged in user would be resolved
	 */
	private String identityIdExpression;

	/**
	 * identityId for the identityType. e.g. userId for USER, or roleName for ROLE
	 */
	private String identityId;

	public IdentityType getIdentityType() {
		return identityType;
	}

	public void setIdentityType(IdentityType identityType) {
		this.identityType = identityType;
	}

	public String getIdentityId() {
		return identityId;
	}

	public void setIdentityId(String identityId) {
		this.identityId = identityId;
	}

	public String getIdentityIdExpression() {
		return identityIdExpression;
	}

	public void setIdentityIdExpression(String identityIdExpression) {
		this.identityIdExpression = identityIdExpression;
	}

	@Override
	public String toString() {
		return "Identity ID: " + getIdentityId() + ", type: " + getIdentityType() + ", expression: " + getIdentityIdExpression();
	}

}