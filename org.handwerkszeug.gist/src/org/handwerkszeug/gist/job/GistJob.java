package org.handwerkszeug.gist.job;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.WorkbenchJob;
import org.handwerkszeug.gist.Activator;
import org.handwerkszeug.gist.Constants;
import org.handwerkszeug.gist.util.StatusUtil;
import org.handwerkszeug.gist.util.Streams;

public class GistJob extends WorkspaceJob {

	protected IFile file;
	protected String txt;

	protected boolean isPrivate;

	public GistJob(IFile file, String txt, boolean isPrivate) {
		super("send to gist");
		this.file = file;
		this.txt = txt;
		this.isPrivate = isPrivate;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		monitor.beginTask("in progress...", 2);
		try {
			final String resultUri = post(monitor);
			if (resultUri != null) {
				new WorkbenchJob("copy to clipboard...") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						monitor.beginTask("copy gist URL to clipboard", 2);
						try {
							monitor.worked(1);
							Clipboard clipboard = new Clipboard(getDisplay());
							clipboard
									.setContents(new Object[] { resultUri },
											new Transfer[] { TextTransfer
													.getInstance() });
							monitor.worked(1);
						} finally {
							monitor.done();
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}
			monitor.worked(1);
		} catch (UnauthorizedException e) {
			openPreferences();
		} catch (Exception e) {
			return StatusUtil.createError(Activator.getDefault(), e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	protected void openPreferences() {
		new WorkbenchJob("") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					PreferenceDialog dialog = PreferencesUtil
							.createPreferenceDialogOn(getDisplay()
									.getActiveShell(), Constants.PAGE_PREF,
									null, null);
					dialog.setMessage("Unauthorized. Name or Token is invalid",
							IMessageProvider.ERROR);
					if (dialog.open() == Window.OK) {
						GistJob.this.schedule();
					}
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	protected String post(IProgressMonitor monitor) throws Exception {
		String result = null;
		HttpClient httpclient = new DefaultHttpClient();
		try {
			HttpPost post = new HttpPost("http://gist.github.com/gists");

			List<NameValuePair> nvs = new ArrayList<NameValuePair>();
			nvs.add(new BasicNameValuePair("login", getLogin()));
			nvs.add(new BasicNameValuePair("token", getToken()));
			if (this.isPrivate) {
				nvs.add(new BasicNameValuePair("action_button", "private"));
			}
			nvs.add(new BasicNameValuePair("file_ext[gistfile1]", this.file
					.getFileExtension()));
			nvs.add(new BasicNameValuePair("file_name[gistfile1]", this.file
					.getName()));
			nvs.add(new BasicNameValuePair("file_contents[gistfile1]", getTxt()));
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nvs, "UTF-8");
			post.setEntity(entity);

			monitor.worked(1);

			HttpResponse response = httpclient.execute(post);
			StatusLine line = response.getStatusLine();
			if (line.getStatusCode() == 302) { // Found
				Header h = response.getFirstHeader("Location");
				result = h.getValue();
			} else if (line.getStatusCode() == 401) { // Unauthorized
				throw new UnauthorizedException();
			}
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
		return result;
	}

	class UnauthorizedException extends RuntimeException {
		private static final long serialVersionUID = 2692412026234876091L;
	}

	protected String getTxt() throws CoreException {
		if (this.txt != null) {
			return this.txt;
		}
		return Streams.readText(this.file.getContents());
	}

	protected String getLogin() {
		return Activator.getPreference(Constants.PREF_LOGIN);
	}

	protected String getToken() {
		return Activator.getPreference(Constants.PREF_TOKEN);
	}
}
