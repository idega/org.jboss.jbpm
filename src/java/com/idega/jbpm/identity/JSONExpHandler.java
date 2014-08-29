package com.idega.jbpm.identity;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.graph.exe.ExecutionContext;

import com.idega.jbpm.bean.XStreamAlias;
import com.idega.jbpm.identity.permission.Access;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.8 $
 *
 *          Last modified: $Date: 2009/02/23 12:38:22 $ by $Author: civilis $
 */
public class JSONExpHandler {

	private static final Logger LOGGER = Logger.getLogger(JSONExpHandler.class.getName());

	private static final String taskAssignment = "taskAssignment",
								rolesAssignment = "rolesAssignment",
								rightsAssignment = "rightsAssignment",
								role = "role",
								identity = "identity",
								access = "access";

	public static TaskAssignment resolveRolesFromJSONExpression(String expression, ExecutionContext context) {
		expression = expression.trim();

		if (expression.length() == 0) {
			LOGGER.warning("Tried to resolve roles from json expression, but expression was empty");
			return null;
		}

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {
				LOGGER.warning("Expression provided does not contain json expression. Expression: " + expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		expression = getResolvedExpression(expression, context);

		return getParsedExpression(
				expression,
				TaskAssignment.class,
				true,
				Arrays.asList(
					new XStreamAlias(taskAssignment, TaskAssignment.class),
					new XStreamAlias(role, Role.class),
					new XStreamAlias(identity, Identity.class),
					new XStreamAlias(access, Access.class)
				)
		);
	}

	public static TaskAssignment resolveRightsRolesFromJSONExpression(String expression, ExecutionContext context) {
		expression = expression.trim();

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {
				LOGGER.warning("Expression provided does not contain json expression. Expression: "	+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		expression = getResolvedExpression(expression, context);

		return getParsedExpression(
				expression,
				TaskAssignment.class,
				true,
				Arrays.asList(
					new XStreamAlias(rightsAssignment, TaskAssignment.class),
					new XStreamAlias(role, Role.class),
					new XStreamAlias(identity, Identity.class),
					new XStreamAlias(access, Access.class)
				)
		);
	}

	public static RolesAssignment resolveRolesAssignment(String expression, ExecutionContext context) {
		expression = expression.trim();

		if (expression.length() == 0) {
			LOGGER.warning("Tried to resolve roles from json expression, but expression was empty");
			return null;
		}

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT)
				|| !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {

			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {
				LOGGER.warning("Expression provided does not contain json expression. Expression: "	+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		expression = getResolvedExpression(expression, context);

		return getParsedExpression(
				expression,
				RolesAssignment.class,
				true,
				Arrays.asList(
					new XStreamAlias(rolesAssignment, RolesAssignment.class),
					new XStreamAlias(role, Role.class),
					new XStreamAlias(identity, Identity.class),
					new XStreamAlias(access, Access.class)
				)
		);
	}

	public static Role resolveRoleFromJSONExpression(String expression, ExecutionContext context) {
		expression = expression.trim();

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {
				LOGGER.warning("Expression provided does not contain json expression. Expression: " + expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		expression = getResolvedExpression(expression, context);

		return getParsedExpression(
				expression,
				Role.class,
				true,
				Arrays.asList(
					new XStreamAlias(role, Role.class),
					new XStreamAlias(identity, Identity.class),
					new XStreamAlias(access, Access.class)
				)
		);
	}

	public static void main(String[] args) {
		String exp = "{rolesAssignment: {roles: {role: ["
				+ "{roleName: \"bpm_parkingcard_handler\", accesses: {access: [read]}, identities: {identity: [{identityType: ROLE, identityIdExpression: \"myrolename\"}]}"
				+ "}, "
				+ "{roleName: \"bpm_parkingcard_owner\", accesses: {access: [read, write]}, scope: PI}"
				+ "]} }}";

		RolesAssignment ta = resolveRolesAssignment(exp, null);

		LOGGER.info("ident=" + ta.getRoles().iterator().next().getIdentities().iterator().next().getIdentityType());
	}

	public static TaskAssignment resolveRightsMgmtForRolesFromJSONExpression(String expression, ExecutionContext context) {
		expression = expression.trim();

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {
				LOGGER.warning("Expression provided does not contain json expression. Expression: "	+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		expression = getResolvedExpression(expression, context);

		return getParsedExpression(
				expression,
				TaskAssignment.class,
				true,
				Arrays.asList(
					new XStreamAlias(rightsAssignment, TaskAssignment.class),
					new XStreamAlias(role, Role.class),
					new XStreamAlias(identity, Identity.class),
					new XStreamAlias(access, Access.class)
				)
		);
	}

	public static String getResolvedExpression(String expression, ExecutionContext context) {
		if (StringUtil.isEmpty(expression) || context == null) {
			return expression;
		}

		if (expression.indexOf("#{") != -1 && expression.indexOf(CoreConstants.CURLY_BRACKET_RIGHT) != -1) {
			while (expression.indexOf("#{") != -1) {
				int start = expression.indexOf("#{");
				int end = -1;
				for (int i = start + 2; i < expression.length(); i++) {
					if (expression.charAt(i) == '}') {
						end = i;
						break;
					}
				}
				if (end > start) {
					String variable = expression.substring(start + 2, end);
					Object value = context.getVariable(variable);
					if (value == null) {
						LOGGER.warning("Unable to get value for variable: '" + variable + "'. Proc. inst. ID: " + context.getProcessInstance().getId());
						value = variable;
					}
					expression = StringHandler.replace(expression, "#{" + variable + CoreConstants.CURLY_BRACKET_RIGHT, (String) value);
				}
			}
		}

		return expression;
	}

	private static <T> T getParsedExpression(String expression, Class<T> typeOf, boolean reTry, List<XStreamAlias> aliases) {
		try {
			XStream xstream = new XStream(new JettisonMappedXmlDriver());
			if (!ListUtil.isEmpty(aliases)) {
				for (XStreamAlias alias: aliases) {
					if (alias == null) {
						continue;
					}

					xstream.alias(alias.getName(), alias.getTheClass());
				}
			}

			return (T) xstream.fromXML(expression);
		} catch (Exception e) {
			String message = "Error parsing expression into " + typeOf.getName() + ", aliases: " + aliases + (reTry ? ". Will re-try to parse expression" : CoreConstants.EMPTY) + ". Expression:\n" + expression;
			LOGGER.log(Level.WARNING, message, e);
			if (reTry) {
				return getParsedExpression(expression, typeOf, false, aliases);
			}

			CoreUtil.sendExceptionNotification(message, e);
		}

		return null;
	}

}