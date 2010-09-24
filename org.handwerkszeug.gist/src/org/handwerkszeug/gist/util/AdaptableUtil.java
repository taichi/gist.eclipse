package org.handwerkszeug.gist.util;

import org.eclipse.core.runtime.IAdaptable;

public class AdaptableUtil {

	@SuppressWarnings("unchecked")
	public static <T> T to(Object adaptable, Class<T> clazz) {
		T result = null;
		if ((adaptable == null) || (clazz == null)) {
			result = null;
		} else if (clazz.isAssignableFrom(adaptable.getClass())) {
			result = (T) adaptable;
		} else if (adaptable instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) adaptable;
			result = (T) a.getAdapter(clazz);
		}
		return result;
	}
}