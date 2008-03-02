/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.idega.jbpm.identity;

import java.util.List;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.identity.assignment.ExpressionAssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * <p>Expects assignment expression in json notation. E.g.:</p>
 * 
 * <p>
 * <code>
 * {taskAssignment: {roles: {role: [
 *	{roleName: handler, accesses: {access: [read, write]}},
 *	{roleName: owner, accesses: {access: [read, write]}} 
 * ]} }}
 * </code>
 * </p>
 *   
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/03/02 18:32:29 $ by $Author: civilis $
 */
public class JSONAssignmentHandler extends ExpressionAssignmentHandler {
	
	private static final long serialVersionUID = 8955094455268141204L;
	
	private static final String taskAssignment = "taskAssignment";
	private static final String role = "role";
	private static final String access = "access";
	
	private class TaskAssignment { List<Role> roles; }
	private class Role { String roleName; List<Access> accesses; }
	private enum Access { read, write }

	public void assign(Assignable assignable, ExecutionContext executionContext) {

		XStream xstream = new XStream(new JettisonMappedXmlDriver());
		xstream.alias(taskAssignment, TaskAssignment.class);
		xstream.alias(role, Role.class);
		xstream.alias(access, Access.class);
		
		TaskAssignment assignmentExp = (TaskAssignment)xstream.fromXML(expression);

		List<Role> roles = assignmentExp.roles;
		
		for (Role role : roles) {
			
			System.out.println("role name: "+role.roleName);
			System.out.println("role accesses: "+role.accesses);
		}
	  }
}