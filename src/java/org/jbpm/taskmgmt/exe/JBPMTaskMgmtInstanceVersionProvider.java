package org.jbpm.taskmgmt.exe;

import org.jbpm.module.exe.JBPMModuleInstanceVersionProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("jbpmTaskMgmtInstanceVersionUpdater")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class JBPMTaskMgmtInstanceVersionProvider extends JBPMModuleInstanceVersionProvider<TaskMgmtInstance> {

}