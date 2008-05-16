package com.idega.jbpm.process.business.messages.resolvers;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueResolver;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/05/16 09:47:42 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class BeanValueResolver implements MessageValueResolver {
	
	private static final String beanKeyStart = "bean:";
	
	public String getResolverType() {
		return "bean";
	}

	public String getValue(String key) {
		
		Logger.getLogger(getClass().getName()).log(Level.WARNING, "getValue(String key) called for BeanValueResolver. getValue(String key, Object context) should be used instead");
		return null;
	}

	public String getValue(String key, MessageValueContext mvCtx) {
		
		//bean:user
		String beanName;
		
		boolean beanOnly = false;
		
		if(key.contains(CoreConstants.DOT)) {
			
			beanName = key.substring(0, key.indexOf(CoreConstants.DOT));
			key = key.substring(key.indexOf(CoreConstants.DOT)+1);
			beanOnly = false;
			
		} else {
			
			beanName = key;
			beanOnly = true;
		}
	
		String shouldStart = beanKeyStart+beanName;
		
		for (Entry<String, Object> entry : mvCtx.entrySet()) {
			
			String entryKey = entry.getKey();
			
			if(entryKey.startsWith(shouldStart)) {
				
				if(beanOnly)
					return entry.getValue().toString();
				else {
		
					try {
						String val = BeanUtils.getProperty(entry.getValue(), key);
						return val;
						
					} catch (Exception e) {
						Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving property from object: "+entry.getValue()+", property: "+key);
					}
				}
			}
		}
		
		return null;
	}
}