package com.idega.jbpm.variables;

import java.util.Collection;

import com.idega.core.business.DefaultSpringBean;

public abstract class VariablesPreloader extends DefaultSpringBean {

	public static final String BEAN_NAME_PREFIX = "bpmVariableValuePreloader";

	public abstract void preloadForCaseIds(Collection<Integer> caseIds);

	public abstract void preloadForProcessIds(Collection<Long> processIds);

}