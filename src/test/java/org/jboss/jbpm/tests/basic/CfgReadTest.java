package org.jboss.jbpm.tests.basic;

import java.io.InputStream;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.util.ClassLoaderUtil;

import junit.framework.TestCase;

/**
 * 
 * @author <a href="civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/13 11:21:23 $ by $Author: civilis $
 *
 */
public class CfgReadTest extends TestCase {
	
	public void setUp() {
	}
  
	public void testMainScenario() {
		
		if(true)
			return;
		String cfgLocation = "org/jboss/jbpm/tests/basic/jbpm.cfg.xml";
		System.out.println("wtf: "+cfgLocation);
		
		InputStream jbpmCfgXmlStream = ClassLoaderUtil.getStream(cfgLocation);
		
		System.out.println("jbpmCfgXmlStream: "+jbpmCfgXmlStream);
		JbpmConfiguration cfg = JbpmConfiguration.getInstance(cfgLocation);
		System.out.println("cfg: "+cfg);
		cfg.createSchema();
		JbpmContext ctx = cfg.createJbpmContext();
		System.out.println("ctx: "+ctx);
	}
}