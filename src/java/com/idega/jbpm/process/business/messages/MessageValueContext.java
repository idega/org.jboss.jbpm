package com.idega.jbpm.process.business.messages;

import java.util.HashMap;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/16 09:47:41 $ by $Author: civilis $
 */
public class MessageValueContext extends HashMap<String, Object> {

	private static final long serialVersionUID = -572272469613507688L;
	
	public MessageValueContext() {
		super();
	}
	
	public MessageValueContext(int cnt) {
		super(cnt);
	}
}