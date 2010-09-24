package org.handwerkszeug.gist.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.handwerkszeug.gist.Activator;
import org.handwerkszeug.gist.Constants;

public class GistPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	protected StringFieldEditor login;
	protected StringFieldEditor token;

	public GistPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	public void init(IWorkbench workbench) {
		// do nothing.
	}

	@Override
	protected void createFieldEditors() {
		this.login = new StringFieldEditor(Constants.PREF_LOGIN, "Name :",
				getFieldEditorParent());
		this.login.setEmptyStringAllowed(false);
		addField(this.login);
		this.token = new StringFieldEditor(Constants.PREF_TOKEN, "API Token :",
				getFieldEditorParent());
		this.token.setEmptyStringAllowed(false);
		addField(this.token);
	}

}
