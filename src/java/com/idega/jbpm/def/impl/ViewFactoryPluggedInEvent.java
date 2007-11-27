package com.idega.jbpm.def.impl;

import org.springframework.context.ApplicationEvent;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/11/27 16:33:26 $ by $Author: civilis $
 */
public class ViewFactoryPluggedInEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1577815015990328896L;

	public ViewFactoryPluggedInEvent(Object source) {
        super(source);
    }
}