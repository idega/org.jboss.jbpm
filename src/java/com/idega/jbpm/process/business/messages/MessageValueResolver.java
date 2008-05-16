package com.idega.jbpm.process.business.messages;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/16 09:47:41 $ by $Author: civilis $
 */
public interface MessageValueResolver {

	public abstract String getResolverType();
	public abstract String getValue(String key, MessageValueContext context);
}