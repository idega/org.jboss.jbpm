package com.idega.jbpm.exe;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.webface.WFUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/03/17 12:19:00 $ by $Author: civilis $
 */
public class EndProcessHandler implements ActionHandler {

	private static final long serialVersionUID = -4463378796598145701L;
	private static final String idegaJbpmContextBeanIdentifier = "idegaJbpmContext";
	
	public EndProcessHandler() { }
	
	public EndProcessHandler(String parm) { }

	public void execute(ExecutionContext ctx) throws Exception {

		ctx.getProcessInstance().end();
		
		@SuppressWarnings("unchecked")
		List<Token> tokens = ctx.getProcessInstance().findAllTokens();
		
		for (Token token : tokens) {
			
			if(!token.hasEnded()) {
				token.end();
			}
		}
	}
	
	protected IdegaJbpmContext getIdegaJbpmContext() {
		
		return (IdegaJbpmContext)WFUtil.getBeanInstance(idegaJbpmContextBeanIdentifier);
	}
}