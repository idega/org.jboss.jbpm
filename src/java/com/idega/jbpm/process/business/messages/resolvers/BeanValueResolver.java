package com.idega.jbpm.process.business.messages.resolvers;

import java.util.logging.Level;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueResolver;
import com.idega.util.CoreConstants;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.3 $
 *
 * Last modified: $Date: 2008/08/08 16:16:55 $ by $Author: civilis $
 */
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class BeanValueResolver extends DefaultSpringBean implements MessageValueResolver {

	private static final String beanType = "bean";

	@Override
	public String getResolverType() {
		return beanType;
	}

	@Override
	public String getValue(String key, MessageValueContext mvCtx) {
		String beanName;

		boolean beanOnly = false;
		if (key.contains(CoreConstants.DOT)) {
			beanName = key.substring(0, key.indexOf(CoreConstants.DOT));
			key = key.substring(key.indexOf(CoreConstants.DOT)+1);
			beanOnly = false;
		} else {
			beanName = key;
			beanOnly = true;
		}

		Object mvVal = mvCtx.getValue(beanType, beanName);
		if (mvVal == null) {
			getLogger().warning("There is no value for " + beanName);
			return null;
		}

		if (beanOnly) {
			return mvVal.toString();
		} else {
			try {
				String val = BeanUtils.getProperty(mvVal, key);
				return val;
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Exception while resolving property from object: " + mvVal + ", property: " + key, e);
			}
		}

		return null;
	}
}