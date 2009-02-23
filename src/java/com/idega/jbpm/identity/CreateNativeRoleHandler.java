package com.idega.jbpm.identity;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.accesscontrol.business.AccessController;
import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.exe.BPMFactory;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/02/23 13:18:54 $ by $Author: civilis $
 */
@Service("createNativeRoleHandler")
@Scope("prototype")
public class CreateNativeRoleHandler implements ActionHandler {
	
	private static final long serialVersionUID = -7291510841831153317L;
	@Autowired
	private BPMFactory bpmFactory;
	private String nativeRoleName;
	
	public void execute(ExecutionContext ctx) throws Exception {
		
		String nativeRoleName = getNativeRoleName();
		
		if (!StringUtil.isEmpty(nativeRoleName)) {
			
			AccessController ac = getAccessController();
			ac.checkIfRoleExistsInDataBaseAndCreateIfMissing(nativeRoleName);
			
		} else {
			Logger
			        .getLogger(getClass().getName())
			        .log(Level.WARNING,
			            "Called createNativeRoleHander, but no nativeRoleName provided");
		}
	}
	
	BPMFactory getBpmFactory() {
		return bpmFactory;
	}
	
	public String getNativeRoleName() {
		return nativeRoleName;
	}
	
	public void setNativeRoleName(String nativeRoleName) {
		this.nativeRoleName = nativeRoleName;
	}
	
	private AccessController getAccessController() {
		
		return IWMainApplication.getDefaultIWMainApplication()
		        .getAccessController();
	}
}