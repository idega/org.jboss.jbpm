package org.jboss.jbpm;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/16 09:47:41 $ by $Author: civilis $
 */
public class IWBundleStarter implements IWBundleStartable {
	
	public static final String IW_BUNDLE_IDENTIFIER = "org.jboss.jbpm";
	
	public void start(IWBundle starterBundle) {
		BPMViewManager vm = BPMViewManager.getInstance(starterBundle.getApplication());
		vm.initializeStandardNodes(starterBundle);
	}

	public void stop(IWBundle starterBundle) { }
}
