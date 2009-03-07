package com.idega.jbpm.subprocess;

import java.util.List;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/03/07 13:30:01 $ by $Author: civilis $
 */
@Service("findRunningSubprocess")
@Scope("prototype")
public class FindRunningSubprocessHandler implements ActionHandler {
	
	private static final long serialVersionUID = 7194887763914076687L;
	
	private String variableNameForSubProcessInstanceId;
	private String subprocessName;
	@Autowired
	private BPMDAO bpmDAO;
	
	public void execute(ExecutionContext ectx) throws Exception {
		
		String subprocessName = getSubprocessName();
		String variableNameForSubProcessInstanceId = getVariableNameForSubProcessInstanceId();
		
		if (StringUtil.isEmpty(subprocessName)
		        || StringUtil.isEmpty(variableNameForSubProcessInstanceId)) {
			
			throw new IllegalArgumentException(
			        "Not all parameters provided: subprocessName="
			                + subprocessName
			                + ", variableNameForSubProcessInstanceId="
			                + variableNameForSubProcessInstanceId);
		}
		
		Long processInstanceId = ectx.getProcessInstance().getId();
		
		List<ProcessInstance> subProcesses = getBpmDAO()
		        .getSubprocessInstancesOneLevel(processInstanceId);
		
		Long subProcessInstanceId = null;
		
		if (!ListUtil.isEmpty(subProcesses)) {
			
			for (ProcessInstance subProcess : subProcesses) {
				
				if (!subProcess.hasEnded()
				        && subprocessName.equals(subProcess.getProcessDefinition()
				                .getName())) {
					
					subProcessInstanceId = subProcess.getId();
					
					// we are ignoring the case, when there are more than one active subprocess of
					// the same name
					break;
				}
			}
		}
				
		// we are setting null here too, so that we override the previous value if it was present
		ectx.setVariable(variableNameForSubProcessInstanceId,
		    subProcessInstanceId);
	}
	
	public String getVariableNameForSubProcessInstanceId() {
		return variableNameForSubProcessInstanceId;
	}
	
	public void setVariableNameForSubProcessInstanceId(
	        String variableNameForSubProcessInstanceId) {
		this.variableNameForSubProcessInstanceId = variableNameForSubProcessInstanceId;
	}
	
	public String getSubprocessName() {
		return subprocessName;
	}
	
	public void setSubprocessName(String subprocessName) {
		this.subprocessName = subprocessName;
	}
	
	BPMDAO getBpmDAO() {
		return bpmDAO;
	}
}