package org.handwerkszeug.gist.handlers;


/**
 * @author taichi
 */
public class PublicGistHandler extends AbstractGistHandler {

	@Override
	protected boolean isPrivate() {
		return false;
	}
}
