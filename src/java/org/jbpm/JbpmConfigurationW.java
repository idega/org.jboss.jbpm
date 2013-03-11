package org.jbpm;

import java.io.InputStream;
import java.util.Stack;

import javax.persistence.EntityManagerFactory;

import org.jbpm.configuration.ObjectFactory;
import org.jbpm.configuration.ObjectFactoryImpl;
import org.jbpm.configuration.ObjectInfo;
import org.jbpm.configuration.ValueInfo;
import org.jbpm.persistence.db.StaleObjectLogConfigurer;
import org.jbpm.util.ClassLoaderUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When initialized (through constructor method createJbpmConfiguration), this
 * class creates itself and puts to static instances map of jbpmConfiguration.
 * Therefore, when JbpmConfiguration.getInstance method is called,
 * JbpmConfigurationW is returned. We need this, because we can manage, how (and
 * which) jbpmContext is created. Now it is used for adding existing
 * (thread-local) session to jbpm context
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 *          Last modified: $Date: 2009/01/07 18:31:22 $ by $Author: civilis $
 */
public class JbpmConfigurationW extends JbpmConfiguration {

	private static final long serialVersionUID = 8274943833387294127L;
	public static final String mainJbpmContext = "idegaMain";

	private ThreadLocal<Stack<Boolean>> doCommitStackLocal = new ThreadLocal<Stack<Boolean>>();

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	public JbpmConfigurationW(ObjectFactory objectFactory) {
		super(objectFactory);
	}

	@SuppressWarnings("unchecked")
	public static JbpmConfiguration createJbpmConfiguration(String pathToConfiguration) {
		InputStream jbpmCfgXmlStream = ClassLoaderUtil.getStream(pathToConfiguration);

		ObjectFactory objectFactory = parseObjectFactory(jbpmCfgXmlStream);
		JbpmConfiguration cfg = createJbpmConfiguration(objectFactory);

		instances.put("jbpm.cfg.xml", cfg);

		return cfg;
	}

	/**
	 * copied from JbpmConfiguration. Creating JbpmConfigurationW instead of
	 * JbpmConfiguration here
	 *
	 * @param objectFactory
	 * @return
	 */
	protected static JbpmConfiguration createJbpmConfiguration(ObjectFactory objectFactory) {
		// here instantiating JbpmConfigurationW
		JbpmConfigurationW jbpmConfiguration = new JbpmConfigurationW(objectFactory);

		// now we make the bean jbpm.configuration always availble
		if (objectFactory instanceof ObjectFactoryImpl) {
			ObjectFactoryImpl objectFactoryImpl = (ObjectFactoryImpl) objectFactory;
			ObjectInfo jbpmConfigurationInfo = new ValueInfo("jbpmConfiguration", jbpmConfiguration);
			objectFactoryImpl.addObjectInfo(jbpmConfigurationInfo);

			if (getHideStaleObjectExceptions(objectFactory)) {
				StaleObjectLogConfigurer.hideStaleObjectExceptions();
			}
		}

		return jbpmConfiguration;
	}

	/**
	 * copied from JbpmConfiguration
	 *
	 * @param objectFactory
	 * @return
	 */
	private static boolean getHideStaleObjectExceptions(ObjectFactory objectFactory) {
		if (!objectFactory.hasObject("jbpm.hide.stale.object.exceptions"))
			return true;

		Object object = objectFactory.createObject("jbpm.hide.stale.object.exceptions");
		return object instanceof Boolean ? ((Boolean) object).booleanValue() : true;
	}

	@Override
	void jbpmContextCreated(JbpmContext jbpmContext) {
		JbpmContext currentContext = getCurrentJbpmContext();
		if (currentContext != null)
			jbpmContext.setSession(currentContext.getSession());

		super.jbpmContextCreated(jbpmContext);
	}

	public Stack<Boolean> getDoCommitStack() {
		Stack<Boolean> stack = doCommitStackLocal.get();
		if (stack == null) {
			stack = new Stack<Boolean>();
			doCommitStackLocal.set(stack);
		}

		return stack;
	}

	@Override
	public JbpmContext createJbpmContext() {
		JbpmContext context = createJbpmContext(mainJbpmContext);
		return context;
	}

	@Override
	public JbpmContext createJbpmContext(String name) {
		JbpmContext context = super.createJbpmContext(name);
		return context;
	}

	@Override
	void jbpmContextClosed(JbpmContext jbpmContext) {
		super.jbpmContextClosed(jbpmContext);
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}