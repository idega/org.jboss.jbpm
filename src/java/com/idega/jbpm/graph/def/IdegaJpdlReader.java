package com.idega.jbpm.graph.def;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.jbpm.context.def.VariableAccess;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.instantiation.Delegation;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.taskmgmt.def.TaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;

import com.idega.business.IBOLookup;
import com.idega.core.accesscontrol.business.AccessController;
import com.idega.core.accesscontrol.business.StandardRoles;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.utils.JBPMConstants;
import com.idega.slide.business.IWSlideService;
import com.idega.slide.util.AccessControlList;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.LocaleUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.messages.MessageResourceFactory;

/**
 * @author <a href="anton@idega.com">Anton Makarov</a>
 * @version Revision: 1.0 Last modified: Oct 9, 2008 by Author: Anton
 */

public class IdegaJpdlReader extends JpdlXmlReader {
	
	private static final long serialVersionUID = -4956191449989922971L;
	
	private InputSource processDefinitionXMLSource;
	
	@Autowired
	private MessageResourceFactory messageFactory;
	
	public InputSource getProcessDefinitionXMLSource() {
		return processDefinitionXMLSource;
	}
	
	public void setProcessDefinitionXMLSource(
	        InputSource processDefinitionXMLSource) {
		this.processDefinitionXMLSource = processDefinitionXMLSource;
	}
	
	public IdegaJpdlReader(InputSource inputSource) {
		super(inputSource);
		this.inputSource = inputSource;
		
	}
	
	@Override
	public ProcessDefinition readProcessDefinition() {
		ProcessDefinition procDef = super.readProcessDefinition();
		readMapStrings();
		return procDef;
	}
	
	@SuppressWarnings("unchecked")
	protected void readMapStrings() {
		
		Namespace idegaNS = new Namespace("idg", "http://idega.com/bpm");
		
		Element root = document.getRootElement();
		Element rolesElement = root.element(new QName("roles", idegaNS));
		
		if (rolesElement != null) {
			
			List<Element> roles = rolesElement.elements("role");
			
			if (!ListUtil.isEmpty(roles)) {
				
				Map<Locale, Map<Object, Object>> localisedRoles = new HashMap<Locale, Map<Object, Object>>(
				        roles.size());
				HashSet<String> nativeRolesNames = new HashSet<String>(roles
				        .size());
				
				for (Element role : roles) {
					String roleName = role.attributeValue("name");
					
					Element labelsElement = role.element("labels");
					
					if (labelsElement != null) {
						
						List<Element> labels = labelsElement.elements("label");
						
						for (Element roleLabel : labels) {
							String localeStr = roleLabel.attributeValue("lang");
							Locale locale = LocaleUtil.getLocale(localeStr);
							String localizedLabel = roleLabel.getTextTrim();
							
							Map<Object, Object> roleLabels = localisedRoles
							        .get(locale);
							if (roleLabels == null) {
								roleLabels = new HashMap<Object, Object>();
								roleLabels.put(roleName, localizedLabel);
								localisedRoles.put(locale, roleLabels);
							} else {
								roleLabels.put(roleName, localizedLabel);
							}
						}
					}
					
					String createNative = role.attributeValue("createNative");
					
					if ("true".equals(createNative)) {
						nativeRolesNames.add(roleName);
					}
				}
				
				for (Locale locale : localisedRoles.keySet()) {
					getMessageFactory().setLocalisedMessages(
					    localisedRoles.get(locale), null, locale);
				}
				
				if (!nativeRolesNames.isEmpty()) {
					createNativeRoles(getProcessDefinition().getName(),
					    nativeRolesNames);
				}
			}
		}
	}
	
	protected void createNativeRoles(String processName, Set<String> rolesNames) {
		
		if (!rolesNames.isEmpty()) {
			
			// TODO: move this to appropriate place, also store path of the
			// process should be
			// accessible through api
			
			for (String roleName : rolesNames) {
				
				getAccessController()
				        .checkIfRoleExistsInDataBaseAndCreateIfMissing(roleName);
			}
			
			String storePath = new StringBuilder(JBPMConstants.BPM_PATH)
			        .append(CoreConstants.SLASH).append(processName).toString();
			
			ArrayList<String> roleNames = new ArrayList<String>(rolesNames
			        .size());
			
			// TODO: this need to be outside if, i.e. admin still should have access to the folder,
			// even if process doesn't have any roles
			roleNames.add(StandardRoles.ROLE_KEY_ADMIN);
			
			try {
				IWSlideService slideService = (IWSlideService) IBOLookup
				        .getServiceInstance(IWMainApplication
				                .getDefaultIWApplicationContext(),
				            IWSlideService.class);
				
				AccessControlList processFolderACL = slideService
				        .getAccessControlList(storePath);
				
				processFolderACL = slideService.getAuthenticationBusiness()
				        .applyPermissionsToRepository(processFolderACL,
				            roleNames);
				
				slideService.storeAccessControlList(processFolderACL);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private AccessController getAccessController() {
		
		return IWMainApplication.getDefaultIWMainApplication()
		        .getAccessController();
	}
	
	public MessageResourceFactory getMessageFactory() {
		if (messageFactory == null)
			ELUtil.getInstance().autowire(this);
		
		return messageFactory;
	}
	
	/**
	 * we are overriding this to add this behavior: if delegation is specified, we <b>still</b>
	 * provide variableAccesses to task controller Also, if variables are specified for controller
	 * with delegation, we are ignoring variable tags, as delegation configuration
	 */
	@Override
	protected TaskController readTaskController(Element taskControllerElement) {
		TaskController taskController = new TaskController();
		
		if (taskControllerElement.attributeValue("class") != null) {
			Delegation taskControllerDelegation = new Delegation();
			taskControllerDelegation.read(taskControllerElement, this);
			taskController
			        .setTaskControllerDelegation(taskControllerDelegation);
			
			String configuration = taskControllerDelegation.getConfiguration();
			
			if (configuration.contains("<variable")) {
				
				// replacing <variable /> tags in the configuration, as we don't need to inject them
				// as properties (used for VariableAccess only)
				
				configuration = configuration.replaceAll("<variable[^/>]*/>",
				    CoreConstants.EMPTY);
				taskControllerDelegation.setConfiguration(configuration);
			}
			
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = readVariableAccesses(taskControllerElement);
			taskController.setVariableAccesses(variableAccesses);
			
		} else {
			@SuppressWarnings("unchecked")
			List<VariableAccess> variableAccesses = readVariableAccesses(taskControllerElement);
			taskController.setVariableAccesses(variableAccesses);
		}
		return taskController;
	}
}
