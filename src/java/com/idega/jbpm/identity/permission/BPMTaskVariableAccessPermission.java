package com.idega.jbpm.identity.permission;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/27 11:01:10 $ by $Author: civilis $
 */
public interface BPMTaskVariableAccessPermission extends BPMTaskAccessPermission {

	public abstract String getVariableIndentifier();
}