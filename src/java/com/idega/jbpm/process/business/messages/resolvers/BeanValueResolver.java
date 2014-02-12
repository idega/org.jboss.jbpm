package com.idega.jbpm.process.business.messages.resolvers;

import java.util.logging.Level;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.BPMContext;
import com.idega.jbpm.JbpmCallback;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueResolver;
import com.idega.util.CoreConstants;
import com.idega.util.expression.ELUtil;


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

	@Autowired
	private BPMContext bpmContext;

	private BPMContext getBPMContext() {
		if (bpmContext == null)
			ELUtil.getInstance().autowire(this);
		return bpmContext;
	}

	@Transactional(readOnly = true)
	private String getValue(final Object mvVal, final String key) throws Exception {
		return getBPMContext().execute(new JbpmCallback<String>() {

			@Override
			public String doInJbpm(JbpmContext context) throws JbpmException {
				try {
					return BeanUtils.getProperty(mvVal, key);
				} catch (Exception e) {
					String message = "Error getting value from " + mvVal + " by " + key;
					getLogger().log(Level.WARNING, message, e);
					throw new JbpmException(message, e);
				}
			}
		});
	}

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
				String val = getValue(mvVal, key);
				return val;
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Exception while resolving property from object: " + mvVal + ", property: " + key, e);
			}
		}

		return null;
	}
}