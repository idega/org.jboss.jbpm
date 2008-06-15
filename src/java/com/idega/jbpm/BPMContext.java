package com.idega.jbpm;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/06/15 15:56:45 $ by $Author: civilis $
 */
public interface BPMContext {

	public abstract JbpmConfiguration getJbpmConfiguration();

	public abstract JbpmContext createJbpmContext();

	public abstract void closeAndCommit(JbpmContext ctx);
}