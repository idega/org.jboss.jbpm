package com.idega.jbpm.process.business.messages.resolvers;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueResolver;
import com.idega.jbpm.proxy.ScriptEvaluator;
import com.idega.util.CoreConstants;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/02 15:34:17 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class ScriptExpressionResolver implements MessageValueResolver {
	
	private static final String beanType = "script";
	@Autowired
	private ScriptEvaluator scriptEvaluator;
	
	public String getResolverType() {
		return beanType;
	}
	
	public String getValue(String key, MessageValueContext mvCtx) {
		
		String script = key;
		
		// TODO: add check for ${ } symbols
		
		Map<String, Object> scriptInputs = mvCtx.getScriptInputMap("bean");
		
		try {
			Object returned = getScriptEvaluator().evaluate(script,
			    scriptInputs);
			
			return returned != null ? returned.toString() : CoreConstants.EMPTY;
			
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(
			    Level.SEVERE,
			    "Exception while evaluating script by message value. Script expression = "
			            + script + ", script inputs (keys) = "
			            + scriptInputs.keySet(), e);
			return CoreConstants.EMPTY;
		}
	}
	
	ScriptEvaluator getScriptEvaluator() {
		return scriptEvaluator;
	}
}