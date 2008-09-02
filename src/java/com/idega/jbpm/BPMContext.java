package com.idega.jbpm;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/09/02 12:59:27 $ by $Author: civilis $
 */
public interface BPMContext {

	public abstract JbpmConfiguration getJbpmConfiguration();

	public abstract JbpmContext createJbpmContext();

	public abstract void closeAndCommit(JbpmContext ctx);
	
	public abstract void saveProcessEntity(Object entity);
}