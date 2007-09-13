package com.idega.jbpm.exe;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:24 $ by $Author: civilis $
 */
public abstract class AbstractSubmissionHandler implements SubmissionHandler {

	public void submit(TaskInstance ti, Object submissionData) {
		Node unfolded = unfold(submissionData);
		storeUnfoldedToTI(ti, unfolded);
	}
	
	public Object populate(TaskInstance ti, Object objectToPopulate) {
		
		Node unfolded = getUnfoldedFromTI(ti);
		return fold(unfolded, objectToPopulate);
	}
	public abstract String getIdentifier();
	
	protected abstract Node unfold(Object specificData);
	protected abstract Object fold(Node submissionNode, Object objectToPopulate);
	protected abstract Node getUnfoldedFromTI(TaskInstance ti);
	protected abstract void storeUnfoldedToTI(TaskInstance ti, Node submissionNode);
}