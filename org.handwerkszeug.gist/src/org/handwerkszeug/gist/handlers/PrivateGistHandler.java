package org.handwerkszeug.gist.handlers;

/**
 * @author taichi
 */
public class PrivateGistHandler extends AbstractGistHandler {

	@Override
	protected boolean isPrivate() {
		return true;
	}
}
