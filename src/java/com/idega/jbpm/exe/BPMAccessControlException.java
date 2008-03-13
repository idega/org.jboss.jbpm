package com.idega.jbpm.exe;

import java.security.AccessControlException;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/13 21:05:45 $ by $Author: civilis $
 */
public class BPMAccessControlException extends AccessControlException {
	
	private static final long serialVersionUID = 1688643914999903148L;
	private String userFriendlyMessage;
	
    public BPMAccessControlException(String s) {
        super(s);
    }
    
    public BPMAccessControlException(String s, String userFriendlyMessage) {
        super(s);
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    public String getUserFriendlyMessage() {
    	return userFriendlyMessage;
	}
}