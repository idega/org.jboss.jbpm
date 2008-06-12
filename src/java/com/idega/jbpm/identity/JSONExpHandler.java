package com.idega.jbpm.identity;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.idega.jbpm.identity.permission.Access;
import com.idega.util.CoreConstants;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;


/**
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/06/12 18:29:22 $ by $Author: civilis $
 */
public class JSONExpHandler {
	
	private class TaskAssignment { List<Role> roles; }
	
	private static final String taskAssignment = "taskAssignment";
	private static final String rightsAssignment = "rightsAssignment";
	private static final String role = "role";
	private static final String access = "access";
	
	public static List<Role> resolveRolesFromJSONExpression(String expression) {
		
		expression = expression.trim();
		
		if(!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);
			
			if(lbi < 0 || rbi < 0) {
				
				Logger.getLogger(JSONExpHandler.class.getName()).log(Level.WARNING, "Expression provided does not contain json expression. Expression: "+expression);
				return Collections.emptyList();
			}
				
			expression = expression.substring(lbi, rbi+1);
		}
		
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(taskAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);
		
		TaskAssignment assignmentExp = (TaskAssignment)xstream.fromXML(expression);

		return assignmentExp.roles;
	}
	
	public static Role resolveRoleFromJSONExpression(String expression) {
		
		expression = expression.trim();
		
		if(!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);
			
			if(lbi < 0 || rbi < 0) {
				
				Logger.getLogger(JSONExpHandler.class.getName()).log(Level.WARNING, "Expression provided does not contain json expression. Expression: "+expression);
				return null;
			}
				
			expression = expression.substring(lbi, rbi+1);
		}
		
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);
		
		Role role = (Role)xstream.fromXML(expression);

		return role;
	}
	
	public static void main(String[] args) {
	
//		{taskAssignment: {roles: {role: [
//		                                 {roleName: "bpm_handler", accesses: {access: [read, write]}}
		
		String rexp = "{role: {roleName: \"bpm_defendant\", scope: PI}}";
		
		Role r = resolveRoleFromJSONExpression(rexp);
		
		System.out.println(r.getScope());
	}
	
	public static List<Role> resolveRightsMgmtForRolesFromJSONExpression(String expression) {
		
		expression = expression.trim();
		
		if(!expression.startsWith(CoreConstants.CURLY_BRACKET_LEFT) || !expression.endsWith((CoreConstants.CURLY_BRACKET_RIGHT))) {
			
			int lbi = expression.indexOf(CoreConstants.CURLY_BRACKET_LEFT);
			int rbi = expression.lastIndexOf(CoreConstants.CURLY_BRACKET_RIGHT);
			
			if(lbi < 0 || rbi < 0) {
				
				Logger.getLogger(JSONExpHandler.class.getName()).log(Level.WARNING, "Expression provided does not contain json expression. Expression: "+expression);
				return Collections.emptyList();
			}
				
			expression = expression.substring(lbi, rbi+1);
		}
		
		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(rightsAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);
		
		TaskAssignment assignmentExp = (TaskAssignment)xstream.fromXML(expression);

		return assignmentExp.roles;
	}
}