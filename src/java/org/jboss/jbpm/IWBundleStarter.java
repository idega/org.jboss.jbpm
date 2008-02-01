package org.jboss.jbpm;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/02/01 12:19:45 $ by $Author: civilis $
 */
public class IWBundleStarter implements IWBundleStartable {
	
	public static final String IW_BUNDLE_IDENTIFIER = "org.jboss.jbpm";
	
	public void start(IWBundle starterBundle) {
//		JbmpViewManager jbpm_vm = JbmpViewManager.getInstance(starterBundle.getApplication());
//		jbpm_vm.initializeStandardNodes(starterBundle);
	}

	public void stop(IWBundle starterBundle) { }
}
