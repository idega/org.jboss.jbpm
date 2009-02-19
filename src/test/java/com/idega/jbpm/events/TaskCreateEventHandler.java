package com.idega.jbpm.events;

import junit.framework.Assert;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/02/19 13:06:40 $ by $Author: civilis $
 */
public class TaskCreateEventHandler implements ActionHandler {

	private static final long serialVersionUID = 1L;

	public void execute(ExecutionContext ectx) throws Exception {

		System.out.println("______taks created in action handler!!");

		TaskInstance ti = ectx.getTaskInstance();

		System.out.println("ti = " + ti);

		Assert.assertNotNull(ti);
	}
}