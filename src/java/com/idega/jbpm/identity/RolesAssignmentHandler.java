package com.idega.jbpm.identity;

import java.util.List;
import java.util.logging.Logger;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.jbpm.exe.BPMFactory;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/02/23 12:37:54 $ by $Author: civilis $
 */
@Service("rolesAssignmentHandler")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RolesAssignmentHandler implements ActionHandler {

	private static final long serialVersionUID = 5106156786436790124L;

	@Autowired
	private BPMFactory bpmFactory;

	private String assignmentExpression;

	@Override
	@Transactional(readOnly = false)
	public void execute(ExecutionContext ctx) throws Exception {
		if (StringUtil.isEmpty(getAssignmentExpression()))
			return;

		RolesAssignment rolesAssignment = JSONExpHandler.resolveRolesAssignment(getAssignmentExpression(), ctx);
		List<Role> roles = rolesAssignment.getRoles();
		if (ListUtil.isEmpty(roles))
			return;

		JbpmContext context = ctx.getJbpmContext();
		ProcessInstance pi = getBpmFactory().getMainProcessInstance(context, ctx.getProcessInstance().getId());
		getBpmFactory().getRolesManager().createProcessActors(context, roles, pi);

		Session session = context.getSession();
		FlushMode flushMode = session.getFlushMode();
		if (flushMode != null && FlushMode.ALWAYS != flushMode) {
			session.flush();
		}

		for (Role role : roles) {
			if (ListUtil.isEmpty(role.getIdentities()))
				continue;

			for (Identity identity: role.getIdentities()) {
				if (StringUtil.isEmpty(identity.getIdentityId())) {
					if (StringUtil.isEmpty(identity.getIdentityIdExpression()))
						Logger.getLogger(getClass().getName()).warning("Role with no identity id nor identityIdExpression provided. Expression=" +
								getAssignmentExpression());

					if (Identity.currentUser.equals(identity.getIdentityIdExpression())) {
						BPMUser usr = getBpmFactory().getBpmUserFactory().getCurrentBPMUser();
						Integer usrId = usr.getIdToUse();
						identity.setIdentityId(usrId.toString());
					}
				}
			}
		}

		getBpmFactory().getRolesManager().assignIdentities(pi, roles);
	}

	public String getAssignmentExpression() {
		return assignmentExpression;
	}

	public void setAssignmentExpression(String assignmentExpression) {
		this.assignmentExpression = assignmentExpression;
	}

	BPMFactory getBpmFactory() {
		if (bpmFactory == null)
			ELUtil.getInstance().autowire(this);
		return bpmFactory;
	}
}