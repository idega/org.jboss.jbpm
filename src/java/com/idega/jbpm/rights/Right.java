package com.idega.jbpm.rights;

/**
 * this should be used in idega bpm api to represent any permissive right, that any bpm definition or instance allows.
 * For instance, it can be used to check right for process instance.
 * 
 * Please comment for each enum, where it can be used (processinstance, processdefinition, or taskinstance)
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/10/08 18:50:57 $ by $Author: civilis $
 */
public enum Right {

	/**
	 * represents right of process handler. Should be used for process instance only
	 */
	processHandler
}