package com.idega.jbpm.process.business.messages;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/16 18:18:34 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class MessageValueHandler {

	private Map<String, MessageValueResolver> resolvers;
	private static final String messageValueObj = "mv";
	private static final String beanTokenIdentifier = "bean:token";

	@Autowired(required = false)
	public void setValueResolvers(List<MessageValueResolver> resolvers) {
		
		HashMap<String, MessageValueResolver> resolversMap = new HashMap<String, MessageValueResolver>(resolvers.size());
		
		for (MessageValueResolver resolver : resolvers) {
			resolversMap.put(resolver.getResolverType(), resolver);
		}

		this.resolvers = resolversMap;
	}
	
	public String getMessageValue(String resolverType, String key, MessageValueContext context) {
		
		if(MessageValue.defaultResolverType.equals(resolverType))
			return key;
		
		if(resolvers != null) {

			MessageValueResolver resolver = resolvers.get(resolverType);
			
			if(resolver != null) {

				return resolver.getValue(key, context);
			}
		}
		
		return CoreConstants.EMPTY;
	}
	
	public String getFormattedMessage(String unformattedMessage, String messageValuesExp, Token tkn) {
		
		return getFormattedMessage(unformattedMessage, messageValuesExp, tkn, null);
	}
	
	
	
	protected MessageValueContext updateContext(Token tkn, MessageValueContext mvCtx) {
		
		if(!mvCtx.containsKey(beanTokenIdentifier)) {

			mvCtx.put(beanTokenIdentifier, tkn);
		}
		
		return mvCtx;
	}
	
	public String getFormattedMessage(String unformattedMessage, String messageValuesExp, Token tkn, MessageValueContext providedMVCtx) {
		
		MessageValueContext mvCtx = updateContext(tkn, providedMVCtx);
		
		ArrayList<String> msgVals;
		
		if(messageValuesExp != null) {
			
			XStream xstream = new XStream(new JettisonMappedXmlDriver());
			xstream.alias(messageValueObj, MessageValue.class);
			
			@SuppressWarnings("unchecked")
			List<MessageValue> mvals = (List<MessageValue>)xstream.fromXML(messageValuesExp.trim());
			msgVals = new ArrayList<String>(mvals.size());
			
			for (MessageValue messageValue : mvals) {
				
				String val = getMessageValue(messageValue.getType(), messageValue.getValue(), mvCtx);
				msgVals.add(val == null ? CoreConstants.EMPTY : val);
			}
		} else
			msgVals = null;

		if(msgVals != null) {
		
			String formattedMessage = MessageFormat.format(unformattedMessage, msgVals.toArray());
			return formattedMessage;
		} else
			return unformattedMessage;
	}
}