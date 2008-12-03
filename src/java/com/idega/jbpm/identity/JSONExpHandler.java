package com.idega.jbpm.identity;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.jbpm.identity.permission.Access;
import com.idega.util.CoreConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * TODO: most of the method are the same or very similar - refactor
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.7 $
 * 
 *          Last modified: $Date: 2008/12/03 12:04:01 $ by $Author: civilis $
 */
public class JSONExpHandler {

	private static final String taskAssignment = "taskAssignment";
	private static final String rightsAssignment = "rightsAssignment";
	private static final String role = "role";
	private static final String access = "access";

	public static TaskAssignment resolveRolesFromJSONExpression(
			String expression) {

		expression = expression.trim();

		if (expression.length() == 0) {

			Logger
					.getLogger(JSONExpHandler.class.getName())
					.log(Level.WARNING,
							"Tried to resolve roles from json expression, but expression was empty");
			return null;
		}

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT)
				|| !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {

			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {

				Logger.getLogger(JSONExpHandler.class.getName()).log(
						Level.WARNING,
						"Expression provided does not contain json expression. Expression: "
								+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(taskAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);

		TaskAssignment assignmentExp = (TaskAssignment) xstream
				.fromXML(expression);

		return assignmentExp;
	}

	public static TaskAssignment resolveRightsRolesFromJSONExpression(
			String expression) {

		expression = expression.trim();

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT)
				|| !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {

			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {

				Logger.getLogger(JSONExpHandler.class.getName()).log(
						Level.WARNING,
						"Expression provided does not contain json expression. Expression: "
								+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(rightsAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);

		TaskAssignment assignmentExp = (TaskAssignment) xstream
				.fromXML(expression);

		return assignmentExp;
	}

	public static Role resolveRoleFromJSONExpression(String expression) {

		expression = expression.trim();

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT)
				|| !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {

			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {

				Logger.getLogger(JSONExpHandler.class.getName()).log(
						Level.WARNING,
						"Expression provided does not contain json expression. Expression: "
								+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);

		Role role = (Role) xstream.fromXML(expression);

		return role;
	}

	public static void main(String[] args) {
		
		String exp = "{taskAssignment: {rolesFromProcessInstanceId: 123, roles: {role: ["
            +"{roleName: \"bpm_parkingcard_handler\", accesses: {access: [read]}}, "
            +"{roleName: \"bpm_parkingcard_owner\", accesses: {access: [read, write]}, scope: PI}"
            +"]} }}";

		TaskAssignment ta = resolveRolesFromJSONExpression(exp);
		
		System.out.println(ta.getRolesFromProcessInstanceId());
	}

	public static TaskAssignment resolveRightsMgmtForRolesFromJSONExpression(
			String expression) {

		expression = expression.trim();

		if (!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT)
				|| !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {

			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);

			if (lbi < 0 || rbi < 0) {

				Logger.getLogger(JSONExpHandler.class.getName()).log(
						Level.WARNING,
						"Expression provided does not contain json expression. Expression: "
								+ expression);
				return null;
			}

			expression = expression.substring(lbi, rbi + 1);
		}

		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(rightsAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);

		TaskAssignment assignmentExp = (TaskAssignment) xstream
				.fromXML(expression);

		return assignmentExp;
	}
}