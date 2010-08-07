package com.idega.jbpm.process.business.messages.resolvers;

import java.util.Map;
import java.util.logging.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueResolver;
import com.idega.jbpm.proxy.ScriptEvaluator;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/03/02 15:34:17 $ by $Author: civilis $
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ScriptExpressionResolver extends DefaultSpringBean implements MessageValueResolver {
	
	private static final String beanType = "script";
	
	@Autowired
	private ScriptEvaluator scriptEvaluator;
	
	public String getResolverType() {
		return beanType;
	}
	
	public String getValue(String key, MessageValueContext mvCtx) {
		if (StringUtil.isEmpty(key)) {
			getLogger().warning("Expression is not provided!");
			return CoreConstants.EMPTY;
		}
		
		String script = key;
		
		// TODO: add check for ${ } symbols
		Map<String, Object> scriptInputs = mvCtx.getScriptInputMap("bean");
		if (scriptInputs == null || scriptInputs.isEmpty()) {
			getLogger().warning("Unable to resovle expression: '" + key + "': there are no script inputs!");
			return CoreConstants.EMPTY;
		}
		
		try {
			Object returned = getScriptEvaluator().evaluate(script, scriptInputs);
			if (returned == null) {
				getLogger().warning("No result returned for: script expression = " + script + ", script inputs (keys) = " + scriptInputs.keySet());
				return CoreConstants.EMPTY;
			}
			
			return returned.toString();
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while evaluating script by message value. Script expression = " + script + ", script inputs (keys) = "
			            + scriptInputs.keySet(), e);
		}
		return CoreConstants.EMPTY;
	}
	
	ScriptEvaluator getScriptEvaluator() {
		return scriptEvaluator;
	}
}