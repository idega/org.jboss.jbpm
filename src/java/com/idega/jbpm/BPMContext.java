package com.idega.jbpm;

import org.hibernate.FlushMode;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 *          Last modified: $Date: 2008/12/28 12:08:03 $ by $Author: civilis $
 */
public interface BPMContext {

	public abstract JbpmConfiguration getJbpmConfiguration();

	/**
	 * @deprecated - use execute method to communicate with jbpm
	 * @return
	 */
	@Deprecated
	public abstract JbpmContext createJbpmContext();

	/**
	 * @deprecated - use execute method to communicate with jbpm
	 * @return
	 */
	@Deprecated
	public abstract void closeAndCommit(JbpmContext ctx);

	public abstract <T> void saveProcessEntity(T entity);
	public abstract <T> void saveProcessEntity(JbpmContext context, T entity);

	/**
	 * merges entity with current session, resolved from thread bound jbpmContext
	 * You need to have jbpmContext on the thread to call this method
	 * @param any object
	 * @param entity
	 * @return merged object of the same type provided
	 */
	public abstract <T> T mergeProcessEntity(T entity);

	/**
	 * executes code in the callback in hibernate transaction managed by spring.
	 * Only this method should be used to interact with jbpm.
	 *
	 * @param callback
	 *            - code to execute in managed environment
	 * @return anything code returns
	 */
	public abstract <G> G execute(final JbpmCallback<G> callback);
	public abstract <G> G execute(final JbpmCallback<G> callback, FlushMode flushMode);

}