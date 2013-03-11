package com.idega.jbpm.process.business.messages;

import com.idega.util.CoreConstants;

/**
 * represents message value key. The main responsibility is to provide ala namespace for values in
 * the message context. The namespace can, but not need to be used by different handlers
 *
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.2 $ Last modified: $Date: 2009/03/02 15:33:20 $ by $Author: civilis $
 */
public final class TypeRef {

	public TypeRef(String handlerType, String ref) {
		this.handlerType = handlerType;
		this.ref = ref;
	}

	private String handlerType;
	private String ref;

	String getHandlerType() {
		return handlerType == null ? CoreConstants.EMPTY : handlerType;
	}

	String getRef() {
		return ref == null ? CoreConstants.EMPTY : ref;
	}

	@Override
	public int hashCode() {
		return getHandlerType().hashCode() + getRef().hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (!super.equals(obj) && (obj instanceof TypeRef)) {

			return getHandlerType().equals(((TypeRef) obj).getHandlerType())
			        && getRef().equals(((TypeRef) obj).getRef());
		}

		return false;
	}

	@Override
	public String toString() {
		return "Ref: " + ref + ", handler type: " + handlerType;
	}
}