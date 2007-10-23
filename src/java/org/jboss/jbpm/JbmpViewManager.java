package org.jboss.jbpm;


import java.util.ArrayList;
import java.util.Collection;
import javax.faces.context.FacesContext;


import com.idega.core.accesscontrol.business.StandardRoles;
import com.idega.core.view.ApplicationViewNode;
import com.idega.core.view.DefaultViewNode;
import com.idega.core.view.ViewManager;
import com.idega.core.view.ViewNode;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.repository.data.Singleton;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version 1.0
 */
public class JbmpViewManager implements Singleton  {

	private static final String VIEW_MANAGER_KEY = "iw_jbmpviewmanager";
	private static final String VIEW_MANAGER_ID = "Process";
	
	private ViewNode rootNode;
	private IWMainApplication iwma;
	
	private JbmpViewManager(IWMainApplication iwma){
		
		this.iwma = iwma;
	}

	public static synchronized JbmpViewManager getInstance(IWMainApplication iwma) {
		JbmpViewManager jbmp_vm = (JbmpViewManager)iwma.getAttribute(VIEW_MANAGER_KEY);
		
		if(jbmp_vm == null) {
			jbmp_vm = new JbmpViewManager(iwma);
			iwma.setAttribute(VIEW_MANAGER_KEY, jbmp_vm);
	    }
	    return jbmp_vm;
	}	
	
	public static JbmpViewManager getInstance(FacesContext context) {
		return getInstance(IWMainApplication.getIWMainApplication(context));
	}
	
	public ViewManager getViewManager() {
		return ViewManager.getInstance(iwma);
	}
	
	
	public ViewNode getContentNode() {
		IWBundle iwb = iwma.getBundle(IWBundleStarter.IW_BUNDLE_IDENTIFIER);
		
		if(rootNode == null)
			rootNode = initalizeContentNode(iwb);
		
		return rootNode;
	}
	
	public ViewNode initalizeContentNode(IWBundle contentBundle) {
		
		ViewNode root = getViewManager().getWorkspaceRoot();
		DefaultViewNode node = new ApplicationViewNode(VIEW_MANAGER_ID, root);
		Collection<String> roles = new ArrayList<String>();
		roles.add(StandardRoles.ROLE_KEY_BUILDER);
		node.setAuthorizedRoles(roles);
		
		node.setFaceletUri(contentBundle.getFaceletURI("process.xhtml"));
		rootNode = node;
		return rootNode;
	}
	
	public void initializeStandardNodes(IWBundle bundle){
		ViewNode contentNode = initalizeContentNode(bundle);
		
		DefaultViewNode node = new DefaultViewNode(VIEW_MANAGER_ID, contentNode);
		node.setFaceletUri(bundle.getFaceletURI("processDefUpload.xhtml"));
		node.setName(VIEW_MANAGER_ID);
		node.setVisibleInMenus(true);
		
		node = new DefaultViewNode("xforms_workflow", contentNode);
		node.setFaceletUri("/idegaweb/bundles/com.idega.formbuilder.bundle/facelets/xformsWorkflow.xhtml");
		node.setName("XForms Workflow");
		node.setVisibleInMenus(true);
		
		node = new DefaultViewNode("processMgmntMockup", contentNode);
		node.setFaceletUri(bundle.getFaceletURI("processMgmtMockup.xhtml"));
		node.setName("Process Mgmnt Mockup");
		node.setVisibleInMenus(true);
		
		node = new DefaultViewNode("simpleCasesProcess", contentNode);
		node.setFaceletUri(bundle.getFaceletURI("SimpleCasesProcess.xhtml"));
		node.setName("Simple cases process");
		node.setVisibleInMenus(true);
	}
}