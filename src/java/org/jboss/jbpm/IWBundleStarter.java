package org.jboss.jbpm;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2008/05/16 09:47:41 $ by $Author: civilis $
 */
public class IWBundleStarter implements IWBundleStartable {
	
	public static final String IW_BUNDLE_IDENTIFIER = "org.jboss.jbpm";
	
	@Autowired
	private BPMDAO bpmDAO;
	
	public void start(IWBundle starterBundle) {
		BPMViewManager vm = BPMViewManager.getInstance(starterBundle.getApplication());
		vm.initializeStandardNodes(starterBundle);
		
		Thread procVarsBinder = new Thread(new Runnable() {
			public void run() {
				getBpmDAO().bindProcessVariables();
			}
		});
		procVarsBinder.start();
	}

	BPMDAO getBpmDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmDAO;
	}
	
	public void stop(IWBundle starterBundle) { }
}