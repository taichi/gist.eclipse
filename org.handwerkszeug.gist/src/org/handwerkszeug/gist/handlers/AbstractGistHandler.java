package org.handwerkszeug.gist.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.handwerkszeug.gist.Activator;
import org.handwerkszeug.gist.Constants;
import org.handwerkszeug.gist.job.GistJob;
import org.handwerkszeug.gist.util.AdaptableUtil;
import org.handwerkszeug.gist.util.StringUtil;

/**
 * 
 * @author taichi
 * 
 */
public abstract class AbstractGistHandler extends AbstractHandler {

	protected abstract boolean isPrivate();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		IFile file = null;
		if (part != null) {
			IFileEditorInput input = AdaptableUtil.to(part.getEditorInput(),
					IFileEditorInput.class);
			if (input != null) {
				file = input.getFile();
			}
		}

		if (file != null) {
			ISelection s = HandlerUtil.getCurrentSelection(event);
			TextSelection selection = AdaptableUtil.to(s, TextSelection.class);
			String txt = null;
			if (selection != null) {
				txt = selection.getText();
			}
			openPreferences(event);
			new GistJob(file, txt, isPrivate()).schedule();
		}

		return null;
	}

	protected void openPreferences(ExecutionEvent event) {
		String login = Activator.getPreference(Constants.PREF_LOGIN);
		String token = Activator.getPreference(Constants.PREF_TOKEN);
		if (StringUtil.isEmpty(login) || StringUtil.isEmpty(token)) {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
					HandlerUtil.getActiveShell(event), Constants.PAGE_PREF,
					null, null);
			dialog.open();
		}
	}
}
