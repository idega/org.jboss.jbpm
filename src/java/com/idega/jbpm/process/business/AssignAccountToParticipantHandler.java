package com.idega.jbpm.process.business;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/03/24 19:49:30 $ by $Author: civilis $
 */
public class AssignAccountToParticipantHandler implements ActionHandler {

	private static final long serialVersionUID = -4163428065244816522L;
	public static final String participantUserIdVarName = "string:participantUserId";
	
	public AssignAccountToParticipantHandler() { }
	
	public AssignAccountToParticipantHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {

		System.out.println("assigning account to: "+ctx.getVariable(participantUserIdVarName));
	}
}