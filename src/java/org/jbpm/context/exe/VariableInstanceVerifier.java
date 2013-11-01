package org.jbpm.context.exe;

import org.hibernate.Session;

public class VariableInstanceVerifier {

	private static final VariableInstanceVerifier instance = new VariableInstanceVerifier();

	private VariableInstanceVerifier() {}

	public static final VariableInstanceVerifier getInstance() {
		return instance;
	}

	public boolean isVariablePersisted(Session session, VariableInstance variable) {
		if (variable == null) {
			return false;
		}

		if (variable.id > 0) {
			Object var = null;
			try {
				var = session.get(variable.getClass(), variable.id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (var == null) {
				return false;
			} else {
				return true;
			}
		}

//		session.save(variable);
//		Logger.getLogger(getClass().getName()).info("Persisted object: " + variable + ", ID: " + variable.id);
		return false;
	}

}