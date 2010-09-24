package org.handwerkszeug.gist.util;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

/**
 * @author taichi
 * 
 */
public class LogUtil {

	public static void log(Plugin plugin, Throwable throwable) {
		IStatus status = null;
		if (plugin == null) {
			plugin = ResourcesPlugin.getPlugin();
		}
		if (throwable instanceof CoreException) {
			CoreException e = (CoreException) throwable;
			status = e.getStatus();
		} else {
			status = StatusUtil.createError(plugin, Status.ERROR, throwable);
		}
		plugin.getLog().log(status);
	}

	public static void log(Plugin plugin, String msg) {
		IStatus status = StatusUtil.createInfo(plugin, Status.INFO, msg, null);
		plugin.getLog().log(status);
	}

}