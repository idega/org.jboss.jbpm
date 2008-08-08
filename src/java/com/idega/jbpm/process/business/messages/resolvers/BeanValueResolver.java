package com.idega.jbpm.process.business.messages.resolvers;

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
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/08/08 16:16:55 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class BeanValueResolver implements MessageValueResolver {
	
	private static final String beanType = "bean";
	
	public String getResolverType() {
		return beanType;
	}

	public String getValue(String key, MessageValueContext mvCtx) {
		
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
	
		Object mvVal = mvCtx.getValue(beanType, beanName);
		
		if(beanOnly)
			return mvVal.toString();
		else {

			try {
				String val = BeanUtils.getProperty(mvVal, key);
				return val;
				
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception while resolving property from object: "+mvVal+", property: "+key, e);
			}
		}
		
		return null;
	}
}