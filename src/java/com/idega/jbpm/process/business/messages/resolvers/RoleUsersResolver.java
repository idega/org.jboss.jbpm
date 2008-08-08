package com.idega.jbpm.process.business.messages.resolvers;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.jbpm.identity.JSONExpHandler;
import com.idega.jbpm.identity.Role;
import com.idega.jbpm.process.business.messages.MessageValueContext;
import com.idega.jbpm.process.business.messages.MessageValueResolver;
import com.idega.user.data.User;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/08/08 16:16:42 $ by $Author: civilis $
 */
@Scope("singleton")
@Service
public class RoleUsersResolver implements MessageValueResolver {
	
	private static final String beanType = "roleUsers";
	@Autowired
	private BPMFactory bpmFactory;
	
	public String getResolverType() {
		return beanType;
	}

	public String getValue(String key, MessageValueContext mvCtx) {
		
		Role role = JSONExpHandler.resolveRoleFromJSONExpression(key);
		Token token = mvCtx.getValue(MessageValueContext.tokenBean);
		
		Collection<User> users = getBpmFactory().getRolesManager().getAllUsersForRoles(Arrays.asList(new String[] {role.getRoleName()}), token.getProcessInstance().getId());
		
		if(users != null && !users.isEmpty())
			return users.iterator().next().getName();
		else
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "No user found for role="+role.getRoleName());
		
		return null;
	}

	public BPMFactory getBpmFactory() {
		return bpmFactory;
	}

	public void setBpmFactory(BPMFactory bpmFactory) {
		this.bpmFactory = bpmFactory;
	}
}