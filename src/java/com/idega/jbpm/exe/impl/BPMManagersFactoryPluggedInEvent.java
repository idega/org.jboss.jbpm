package com.idega.jbpm.exe.impl;

import org.springframework.context.ApplicationEvent;

import com.idega.jbpm.exe.BPMManagersFactory;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/01/25 15:24:26 $ by $Author: civilis $
 */
public class BPMManagersFactoryPluggedInEvent extends ApplicationEvent {

	private static final long serialVersionUID = 6826239321252514597L;
	
	private BPMManagersFactory concreteBPMManagersCreator;

	public BPMManagersFactoryPluggedInEvent(Object source) {
        super(source);
        
        concreteBPMManagersCreator = (BPMManagersFactory)source;
    }

	public BPMManagersFactory getConcreteBPMManagersCreator() {
		return concreteBPMManagersCreator;
	}
}