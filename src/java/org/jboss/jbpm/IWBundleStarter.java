package org.jboss.jbpm;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.builder.business.BuilderLogicWrapper;
import com.idega.business.IBOLookup;
import com.idega.core.accesscontrol.dao.PermissionDAO;
import com.idega.core.accesscontrol.data.bean.ICRole;
import com.idega.core.persistence.Param;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.search.BPMSearchIndex;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.user.business.GroupBusiness;
import com.idega.user.dao.GroupDAO;
import com.idega.user.data.Group;
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

	@Autowired
	private VariableInstanceQuerier querier;

	@Autowired
	private BPMSearchIndex searchIndex;

	@Autowired
	private PermissionDAO permissionDAO;

	@Autowired
	private GroupDAO groupDAO;

	private BPMSearchIndex getSearchIndexForVariable() {
		if (searchIndex == null)
			ELUtil.getInstance().autowire(this);
		return searchIndex;
	}

	@Override
	public void start(IWBundle starterBundle) {
		doBuildIndexes(starterBundle.getApplication().getSettings());

		//Create the missing roles
		doCreateRoles();

		//Add missing role groups
		if (starterBundle != null) {
			addMissingRoleGroups(starterBundle.getApplication().getIWApplicationContext());
		}

		Thread variablesDataImporter = new Thread(new Runnable() {
			@Override
			public void run() {
				getBpmDAO().importVariablesData();
			}
		});
		variablesDataImporter.start();

		Thread procVarsBinder = new Thread(new Runnable() {
			@Override
			public void run() {
				getVariableInstanceQuerier().doBindProcessVariables();
			}
		});
		procVarsBinder.start();


	}

	private void doCreateRoles() {
		try {
			ICRole role = getPermissionDAO().getSingleResult(ICRole.QUERY_FIND_ROLE_BY_KEY, ICRole.class, new Param("key", JBPMConstants.VISIBLE_CONTACTS_ROLE_NAME));
			if (role == null) {
				getPermissionDAO().createRole(JBPMConstants.VISIBLE_CONTACTS_ROLE_NAME,
											  "ROLE." + JBPMConstants.VISIBLE_CONTACTS_ROLE_NAME + ".description",
											  "ROLE." + JBPMConstants.VISIBLE_CONTACTS_ROLE_NAME + ".name");
			}
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Error creating role " + JBPMConstants.VISIBLE_CONTACTS_ROLE_NAME, e);
		}
	}

	protected void addMissingRoleGroups(IWApplicationContext iwac) {
		boolean clearCache = false;
		try {
			GroupBusiness groupBiz = IBOLookup.getServiceInstance(iwac, GroupBusiness.class);

			Collection<Group> visibleContactsGroups = groupBiz.getGroupsByGroupName(JBPMConstants.VISIBLE_CONTACTS_GROUP_NAME);

			//	Only generate groups if none exist
			if (visibleContactsGroups == null || visibleContactsGroups.isEmpty()){
				Group visibleContactsGroup = groupBiz.createGroup(JBPMConstants.VISIBLE_CONTACTS_GROUP_NAME, "Permission group for visible contacts.",
						groupBiz.getGroupTypeHome().getPermissionGroupTypeString(), true);
				com.idega.user.data.bean.Group visibleContacs = getGroupDAO().findGroup(Integer.valueOf(visibleContactsGroup.getId()));
				iwac.getIWMainApplication().getAccessController().addRoleToGroup(JBPMConstants.VISIBLE_CONTACTS_ROLE_NAME, visibleContacs, iwac);
				clearCache = true;
			}

			if (clearCache) {
				BuilderLogicWrapper builderLogic = ELUtil.getInstance().getBean(BuilderLogicWrapper.SPRING_BEAN_NAME_BUILDER_LOGIC_WRAPPER);
				builderLogic.reloadGroupsInCachedDomain(iwac, null);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	private void doBuildIndexes(IWMainApplicationSettings settings) {
		try {
			if (settings.getBoolean("bpm.auto_index_vars", Boolean.FALSE)) {
				getSearchIndexForVariable().rebuild();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	VariableInstanceQuerier getVariableInstanceQuerier() {
		if (querier == null) {
			ELUtil.getInstance().autowire(this);
		}
		return querier;
	}

	BPMDAO getBpmDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return bpmDAO;
	}

	private PermissionDAO getPermissionDAO() {
		if (permissionDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return permissionDAO;
	}

	private GroupDAO getGroupDAO() {
		if (groupDAO == null)
			ELUtil.getInstance().autowire(this);
		return groupDAO;
	}

	@Override
	public void stop(IWBundle starterBundle) { }
}