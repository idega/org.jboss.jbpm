package com.idega.jbpm.process.business.autoloader;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.idega.idegaweb.IWMainApplication;
import com.idega.jbpm.IdegaJbpmContext;
import com.idega.jbpm.data.AutoloadedProcessDefinition;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.def.ProcessBundle;
import com.idega.jbpm.def.ProcessBundleManager;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2008/05/12 20:09:41 $ by $Author: civilis $
 */
@Scope("prototype")
@Service("BPMAutoDeployable")
public class AutoDeployable {

	private final Logger logger;
	private BPMDAO BPMDAO;
	private ProcessBundleManager processBundleManager;
	
	private ProcessDefinition processDefinition;
	private ProcessBundle processBundle;
	private Boolean needsDeploy;
	private IdegaJbpmContext idegaJbpmContext;
	private Integer autoloadedVersion;
	
	private AutoloadedProcessDefinition apd;
	
	public AutoDeployable() {
		logger = Logger.getLogger(AutoDeployable.class.getName());
	}
	
	protected boolean checkNeedsDeploy(ProcessDefinition processDefinition) {
		
		String pdName = processDefinition.getName();
		
		AutoloadedProcessDefinition apd;
		
		try {
			apd = getBPMDAO().find(AutoloadedProcessDefinition.class, pdName);
		} catch (Exception e) {
			apd = null;
		}
		
		boolean needsDeploy = false;
		
		if(apd == null)
			needsDeploy = true;
		else {
			
			if(!apd.getAutodeployPermitted()) {
				needsDeploy = false;
			} else {
				needsDeploy = processDefinition.getVersion() > apd.getAutoloadedVersion();
			}
		}
		
		this.apd = apd;
		
		return needsDeploy;
	}
	
	public void deploy(IWMainApplication iwma) {
		
		if(getNeedsDeploy()) {
			
			ProcessDefinition processDefinition = getProcessDefinition();
			
			if(processDefinition != null) {
				
				boolean success = false;
				
				if(getProcessBundle() != null) {
					
					logger.log(Level.INFO, "Deploying process bundle. Definition name: "+processDefinition.getName()+", version: "+getAutoloadedVersion());
					
					try {
						getProcessBundleManager().createBundle(getProcessBundle(), iwma);
						success = true;
						logger.log(Level.INFO, "Deployed process bundle: "+processDefinition.getName());
						
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Failed to deploy process bundle", e);
					}
				} else {
				
					logger.log(Level.INFO, "Deploying process definition. Definition name: "+processDefinition.getName()+", version: "+getAutoloadedVersion());
					
					JbpmContext ctx = getIdegaJbpmContext().createJbpmContext();
					
					try {
						ctx.deployProcessDefinition(processDefinition);
						success = true;
						logger.log(Level.INFO, "Deployed process definition: "+processDefinition.getName());
						
					} catch (JbpmException ee) {
						logger.log(Level.WARNING, "Failed to deploy process: "+processDefinition.getName(), ee);
					} finally {
						getIdegaJbpmContext().closeAndCommit(ctx);
					}
				}
				
				if(success)
					updateAutoDeployInfo(processDefinition);
			}
		}
	}
	
	@Transactional(readOnly = true)
	protected void updateAutoDeployInfo(ProcessDefinition processDefinition) {
		
		String pdName = processDefinition.getName();
		
		if(apd == null) {
			try {
				apd = getBPMDAO().find(AutoloadedProcessDefinition.class, pdName);
			} catch (Exception e) {
				apd = null;
			}
		}
		
		if(apd == null) {
			
			apd = new AutoloadedProcessDefinition();
			apd.setProcessDefinitionName(pdName);
			apd.setAutodeployPermitted(true);
			apd.setAutoloadedVersion(getAutoloadedVersion());
			getBPMDAO().persist(apd);
			
		} else {
			
			apd.setAutoloadedVersion(getAutoloadedVersion());
			getBPMDAO().merge(apd);
		}
	}
	
	public BPMDAO getBPMDAO() {
		return BPMDAO;
	}

	@Autowired
	public void setBPMDAO(BPMDAO bpmdao) {
		BPMDAO = bpmdao;
	}
	
	public ProcessDefinition getProcessDefinition() {
		
		if(processDefinition == null) {
			
			if(getProcessBundle() != null) {
				try {
					processDefinition = getProcessBundle().getProcessDefinition();
					
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Exception while resolving process definition from process bundle", e);
				}
			}
			
			if(processDefinition != null)
				setAutoloadedVersion(processDefinition.getVersion());
		}
		
		if(processDefinition == null)
			logger.log(Level.SEVERE, "Autodeployable.getProcessDefinition: Process definition null");
		
		return processDefinition;
	}

	public void setProcessDefinition(ProcessDefinition processDefinition) {
		this.processDefinition = processDefinition;
		
		if(processDefinition != null)
			setAutoloadedVersion(processDefinition.getVersion());
	}

	public ProcessBundle getProcessBundle() {
		return processBundle;
	}

	public void setProcessBundle(ProcessBundle processBundle) {
		this.processBundle = processBundle;
	}

	public Boolean getNeedsDeploy() {
		
		if(needsDeploy == null) {
			
			ProcessDefinition pd = getProcessDefinition();
			needsDeploy = pd != null && checkNeedsDeploy(pd);
		}
		
		return needsDeploy;
	}

	public ProcessBundleManager getProcessBundleManager() {
		return processBundleManager;
	}

	@Autowired
	public void setProcessBundleManager(ProcessBundleManager processBundleManager) {
		this.processBundleManager = processBundleManager;
	}

	public IdegaJbpmContext getIdegaJbpmContext() {
		return idegaJbpmContext;
	}

	@Autowired
	public void setIdegaJbpmContext(IdegaJbpmContext idegaJbpmContext) {
		this.idegaJbpmContext = idegaJbpmContext;
	}
	
	@Override
	public int hashCode() {
		
		return getProcessDefinition().getName().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(super.equals(obj))
			return true;
		
		if(obj instanceof AutoDeployable) {
			
			return getProcessDefinition().getName().equals(((AutoDeployable)obj).getProcessDefinition().getName());
		}
		return false;
	}

	public Integer getAutoloadedVersion() {
		
		if(autoloadedVersion == null) {
			
			ProcessDefinition pd = getProcessDefinition();
			
			if(pd != null)
				autoloadedVersion = pd.getVersion();
		}
		
		return autoloadedVersion;
	}

	public void setAutoloadedVersion(Integer autoloadedVersion) {
		this.autoloadedVersion = autoloadedVersion;
	}
}