package org.jboss.jbpm.tests.basic;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.jbpm.graph.def.ProcessDefinition;

import com.idega.jbpm.business.JbpmProcessBusinessBean;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2007/09/27 16:27:15 $ by $Author: civilis $
 */
public class ProcessMgmtMockupBean {
	
	private List<SelectItem> processes = new ArrayList<SelectItem>();
	private String processId;

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}
	
	public List<SelectItem> getProcesses() {
		processes.clear();
		processes.add(new SelectItem("", "--Select a process--"));
		List<ProcessDefinition> pdList = getJbpmProcessBusiness().getProcessList();
		
		for (ProcessDefinition processDefinition : pdList)
			processes.add(new SelectItem(String.valueOf(processDefinition.getId()), processDefinition.getName()));
		
		return processes;
	}

	private JbpmProcessBusinessBean jbpmProcessBusiness;

	public JbpmProcessBusinessBean getJbpmProcessBusiness() {
		return jbpmProcessBusiness;
	}

	public void setJbpmProcessBusiness(JbpmProcessBusinessBean jbpmProcessBusiness) {
		this.jbpmProcessBusiness = jbpmProcessBusiness;
	}
}