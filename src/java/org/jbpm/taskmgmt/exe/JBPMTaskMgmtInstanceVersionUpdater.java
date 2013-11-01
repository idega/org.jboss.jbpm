package org.jbpm.taskmgmt.exe;

import org.jbpm.module.exe.JBPMModuleInstanceVersionUpdater;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("jbpmTaskMgmtInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTaskMgmtInstanceVersionUpdater extends JBPMModuleInstanceVersionUpdater<TaskMgmtInstance> {

}