package com.idega.jbpm.process.business;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @deprecated not needed, just add end-complete-process on the end state node
 * TODO: does this end subprocess tokens too?
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 *          Last modified: $Date: 2008/12/03 10:04:54 $ by $Author: civilis $
 */
@Deprecated
@Service("endProcessHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class EndProcessHandler implements ActionHandler {

	private static final long serialVersionUID = -4463378796598145701L;

	@Override
	public void execute(ExecutionContext ctx) throws Exception {
		ctx.getProcessInstance().end();

		@SuppressWarnings("unchecked")
		List<Token> tokens = ctx.getProcessInstance().findAllTokens();

		for (Token token : tokens) {

			if (!token.hasEnded()) {
				token.end();
			}
		}
	}
}