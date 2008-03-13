package com.idega.jbpm.exe;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/13 21:05:45 $ by $Author: civilis $
 */
public class ProcessException extends RuntimeException {
	
	private static final long serialVersionUID = 1688643914999903148L;
	private String userFriendlyMessage;
	
	public ProcessException() {
		super();
    }

    public ProcessException(String s) {
        super(s);
    }
    
    public ProcessException(String s, String userFriendlyMessage) {
        super(s);
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    public ProcessException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ProcessException(String s, Throwable throwable, String userFriendlyMessage) {
        super(s, throwable);
        this.userFriendlyMessage = userFriendlyMessage;
    }

    public ProcessException(Throwable throwable) {
        super(throwable);
    }
    
    public ProcessException(Throwable throwable, String userFriendlyMessage) {
        super(throwable);
        this.userFriendlyMessage = userFriendlyMessage;
    }
    
    public String getUserFriendlyMessage() {
    	return userFriendlyMessage;
	}
}