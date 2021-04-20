package com.idega.jbpm.variables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.faces.component.UIComponent;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.bpm.model.VariableInstance;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.core.business.DefaultSpringBean;
import com.idega.jbpm.bean.VariableInstanceInfo;
import com.idega.jbpm.data.VariableInstanceQuerier;
import com.idega.jbpm.data.dao.BPMDAO;
import com.idega.jbpm.utils.JSONUtil;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public abstract class MultipleSelectionVariablesResolver extends DefaultSpringBean {

	public static final String BEAN_NAME_PREFIX = "bpmVariableValueResolver";

	@Autowired
	private VariableInstanceQuerier variablesQuerier;

	@Autowired
	private BPMDAO bpmDAO;

	private JSONUtil jsonUtil;

	protected Collection<AdvancedProperty> values;

	private Map<Long, Integer> procInstIdsAndCasesIds;

	public abstract Collection<AdvancedProperty> getValues(String procDefId, String variableName);

	protected JSONUtil getJSONUtil() {
		if (jsonUtil == null) {
			jsonUtil = new JSONUtil();
		}
		return jsonUtil;
	}

	protected String getProcessDefNameByProcessDefId(String procDefId) {
		if (StringUtil.isEmpty(procDefId)) {
			return null;
		}

		try {
			return getBpmDAO().getProcessDefinitionNameByProcessDefinitionId(Long.valueOf(procDefId));
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error getting process definition name by id: " + procDefId, e);
		}

		return null;
	}

	protected Collection<VariableInstance> getVariables(String procDefName, String variableName) {
		if (StringUtil.isEmpty(procDefName) || StringUtil.isEmpty(variableName)) {
			return null;
		}

		Collection<Long> procInstIds = getBpmDAO().getProcessInstanceIdsByProcessDefinitionNames(Arrays.asList(procDefName));
		if (ListUtil.isEmpty(procInstIds)) {
			return null;
		}

		return getVariablesQuerier().getVariablesByProcessInstanceIdAndVariablesNames(procInstIds, Arrays.asList(variableName), false);
	}

	public Collection<AdvancedProperty> getBinaryVariablesValues(Collection<VariableInstance> vars) {
		return getBinaryVariablesValues(vars, Collections.emptyList());
	}

	public <V extends com.idega.bpm.model.VariableInstance> Collection<AdvancedProperty> getBinaryVariablesValues(Collection<V> vars, Collection<?> values) {
		if (ListUtil.isEmpty(vars)) {
			return null;
		}

		List<String> addedValues = new ArrayList<>();
		Collection<AdvancedProperty> results = new ArrayList<>();
		for (VariableInstance var: vars) {
			Serializable value = var.getVariableValue();
			if (!(value instanceof Collection)) {
				continue;
			}

			Collection<AdvancedProperty> tmp = getValues((Collection<?>) value);
			if (tmp != null) {
				for (AdvancedProperty prop: tmp) {
					if (!addedValues.contains(prop.toString())) {
						results.add(prop);
						addedValues.add(prop.toString());
					}
				}
			}
		}
		return results;
	}

	protected Collection<AdvancedProperty> getValues(Collection<?> entries) {
		if (ListUtil.isEmpty(entries)) {
			return null;
		}

		Collection<AdvancedProperty> results = new ArrayList<>();
		for (Object entry: entries) {
			if (entry == null) {
				continue;
			}

			AdvancedProperty prop = getValueFromString(entry.toString());
			if (prop != null) {
				results.add(prop);
			}
		}
		return results;
	}

	private Object getParsedFromJSON(String value) {
		try {
			if (StringUtil.isEmpty(value)) {
				return null;
			}

			if (value.startsWith(CoreConstants.CURLY_BRACKET_LEFT)) {
				return getJSONUtil().convertToObject(value);
			}
		} catch (Throwable e) {
			getLogger().log(Level.WARNING, "Error converting JSON string ('" + value + "') to object", e);
		}
		return value;
	}

	protected AdvancedProperty getValueFromString(String value) {
		AdvancedProperty result = null;
		Object obj = getParsedFromJSON(value);
		if (obj instanceof Map) {
			Object id = ((Map<?, ?>) obj).get(getIdKey());
			if (id instanceof String && !StringUtil.isEmpty((String) id)) {
				Object realValue = ((Map<?, ?>) obj).get(getValueKey());
				result = new AdvancedProperty((String) id, (String) realValue);
			}
		} else if (obj instanceof String) {
			result = new AdvancedProperty((String) obj, (String) obj);
		}
		return result;
	}

	public String getIdKey() {
		return null;
	}

	public String getValueKey() {
		return null;
	}

	public String getPresentation(VariableInstance variable) {
		if (variable == null || variable.getVariableValue() == null) {
			return CoreConstants.MINUS;
		}

		return getPresentation(variable.getVariableValue().toString());
	}

	protected VariableInstanceQuerier getVariablesQuerier() {
		if (variablesQuerier == null) {
			ELUtil.getInstance().autowire(this);
		}

		return variablesQuerier;
	}

	protected void setVariablesQuerier(VariableInstanceQuerier variablesQuerier) {
		this.variablesQuerier = variablesQuerier;
	}

	protected BPMDAO getBpmDAO() {
		if (bpmDAO == null) {
			ELUtil.getInstance().autowire(this);
		}

		return bpmDAO;
	}

	protected void setBpmDAO(BPMDAO bpmDAO) {
		this.bpmDAO = bpmDAO;
	}

	protected void addEmptyLabel(String bundleIdentifier) {
		if (values == null) {
			values = new ArrayList<>();
		}

		values.add(new AdvancedProperty(String.valueOf(-1),
				getResourceBundle(getBundle(bundleIdentifier)).getLocalizedString(getNoValuesLocalizationKey(), getNoValuesDefaultString())));
	}

	protected abstract String getNoValuesLocalizationKey();
	protected abstract String getNoValuesDefaultString();

	public abstract String getPresentation(String value);

	public String getPresentation(String name, String value, Long procInstId) {
		return getPresentation(value);
	}
	public String getPresentation(String name, String caseId) {
		return name;
	}

	public String getKeyPresentation(Long procInstId, String key) {
		return StringUtil.isEmpty(key) ? CoreConstants.MINUS : key;
	}
	public String getKeyPresentation(Integer caseId, String key) {
		return getKeyPresentation(Long.valueOf(-1), key);
	}

	public String getPresentation(VariableInstanceInfo variable) {
		if (variable == null || variable.getValue() == null) {
			return CoreConstants.MINUS;
		}

		Object value = variable.getValue();
		if (value instanceof String) {
			return getPresentation((String) value);
		} else if (value instanceof Collection) {
			Collection<?> values = variable.getValue();
			return getPresentation(values);
		} else if (value != null) {
			return getPresentation(value.toString());
		}

		return CoreConstants.MINUS;
	}

	protected String getPresentation(Collection<?> values) {
		if (ListUtil.isEmpty(values)) {
			return null;
		}

		String presentation = CoreConstants.EMPTY;
		for (Iterator<?> valuesIter = values.iterator(); valuesIter.hasNext();) {
			Object value = valuesIter.next();
			if (value == null) {
				continue;
			}

			String realValue = getPresentation(value.toString());
			if (!StringUtil.isEmpty(realValue) && !CoreConstants.MINUS.equals(realValue)) {
				presentation = presentation.concat(realValue);
			} else {
				presentation = presentation.concat(value.toString());
			}
			if (valuesIter.hasNext()) {
				presentation = presentation.concat(CoreConstants.COMMA).concat(CoreConstants.SPACE);
			}
		}
		return presentation;
	}

	public <V extends com.idega.bpm.model.VariableInstance> Collection<V> getFinalSearchResult() {
		return null;
	}

	public Class<? extends UIComponent> getPresentationClass() {
		return DropdownMenu.class;
	}

	public boolean isValueUsedForExport() {
		return true;
	}

	public boolean isValueUsedForCaseList() {
		return true;
	}

	protected String getValueFromBrackets(String value) {
		return StringUtil.getValueFromBrackets(value);
	}

	public Map<Long, Integer> getProcInstIdsAndCasesIds() {
		return procInstIdsAndCasesIds;
	}

	public void setProcInstIdsAndCasesIds(Map<Long, Integer> procInstIdsAndCasesIds) {
		this.procInstIdsAndCasesIds = procInstIdsAndCasesIds;
	}

}