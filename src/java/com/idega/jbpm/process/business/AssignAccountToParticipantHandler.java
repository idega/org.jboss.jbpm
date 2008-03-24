package com.idega.jbpm.process.business;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/03/24 17:23:21 $ by $Author: civilis $
 */
public class AssignAccountToParticipantHandler implements ActionHandler {

	private static final long serialVersionUID = -2378842409705431642L;
	
	public static final String participantUserIdVarName = "string:participantUserId";
	
	public AssignAccountToParticipantHandler() { }
	
	public AssignAccountToParticipantHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {

		System.out.println("assigning account to: "+ctx.getVariable(participantUserIdVarName));
	}
}