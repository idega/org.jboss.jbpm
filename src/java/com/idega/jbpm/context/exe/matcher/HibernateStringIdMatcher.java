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
package com.idega.jbpm.context.exe.matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.StringType;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.JbpmTypeMatcher;

import com.idega.jbpm.IdegaJbpmContext;
import com.idega.util.expression.ELUtil;

/**
 * copied from jbpm. changes:
 * idega-like resolving of jbpmContext
 * session factory resolved from ctx.getSession, than directly from ctx.getSessionFactory
 *
 */
public class HibernateStringIdMatcher implements JbpmTypeMatcher {

	private static final long serialVersionUID = 9098106085272332225L;

	public boolean matches(Object value) {
	  
		IdegaJbpmContext ijctx = ELUtil.getInstance().getBean(IdegaJbpmContext.beanIdentifier);
		JbpmContext jctx = ijctx.createJbpmContext();
		
		try {
			
			boolean matches = false;
		    if (jctx != null) {
		    	@SuppressWarnings("unchecked")
		      Class valueClass = value.getClass();
		      if (value instanceof HibernateProxy) {
		        valueClass = valueClass.getSuperclass();
		      }
	
		      SessionFactory sessionFactory = jctx.getSession().getSessionFactory();
		      if (sessionFactory!=null) {
		        ClassMetadata classMetadata = sessionFactory.getClassMetadata(valueClass);
		        matches = ( (classMetadata!=null)
		                    && (classMetadata.getIdentifierType().getClass()==StringType.class)
		                   );
		      }
		    } else {
		      log.debug("no current context so valueClass cannot be stored as a string-id-ref to a hibernate object");
		      matches = false;
		    }
		    return matches;
			
		} finally {
			ijctx.closeAndCommit(jctx);
		}
	}

	private static Log log = LogFactory.getLog(HibernateStringIdMatcher.class);
}
