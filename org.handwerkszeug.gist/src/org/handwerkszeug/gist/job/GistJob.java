package org.handwerkszeug.gist.job;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.handwerkszeug.gist.util.LogUtil;
import org.handwerkszeug.gist.util.StatusUtil;
import org.handwerkszeug.gist.util.Streams;
import org.handwerkszeug.gist.util.StringUtil;

public class GistJob extends WorkspaceJob {

	static final Set<String> extensions = new HashSet<String>();
	static {
		extensions.add(".txt");
		extensions.add(".as");
		extensions.add(".c");
		extensions.add(".cs");
		extensions.add(".cpp");
		extensions.add(".css");
		extensions.add(".cl");
		extensions.add(".diff");
		extensions.add(".hrl");
		extensions.add(".html");
		extensions.add(".hs");
		extensions.add(".java");
		extensions.add(".js");
		extensions.add(".lua");
		extensions.add(".m");
		extensions.add(".php");
		extensions.add(".pl");
		extensions.add(".py");
		extensions.add(".rb");
		extensions.add(".sql");
		extensions.add(".scala");
		extensions.add(".sls");
		extensions.add(".tex");
		extensions.add(".xml");
		extensions.add(".ascx");
		extensions.add(".scpt");
		extensions.add(".arc");
		extensions.add(".asm");
		extensions.add(".bat");
		extensions.add(".befunge");
		extensions.add(".boo");
		extensions.add(".b");
		extensions.add(".c-objdump");
		extensions.add(".clj");
		extensions.add(".coffee");
		extensions.add(".cfm");
		extensions.add(".cppobjdump");
		extensions.add(".feature");
		extensions.add(".d");
		extensions.add(".d  -objdump");
		extensions.add(".darcspatch");
		extensions.add(".pas");
		extensions.add(".dylan");
		extensions.add(".e");
		extensions.add(".fs");
		extensions.add(".f");
		extensions.add(".s");
		extensions.add(".kid");
		extensions.add(".po");
		extensions.add(".go");
		extensions.add(".man");
		extensions.add(".groovy");
		extensions.add(".mustache");
		extensions.add(".erb");
		extensions.add(".phtml");
		extensions.add(".hx");
		extensions.add(".haml");
		extensions.add(".ini");
		extensions.add(".weechatlog");
		extensions.add(".io");
		extensions.add(".jsp");
		extensions.add(".ll");
		extensions.add(".lhs");
		extensions.add(".mak");
		extensions.add(".mao");
		extensions.add(".mkd");
		extensions.add(".matlab");
		extensions.add(".mxt");
		extensions.add(".md");
		extensions.add(".moo");
		extensions.add(".myt");
		extensions.add(".numpy");
		extensions.add(".objdump");
		extensions.add(".j");
		extensions.add(".pir");
		extensions.add(".pd");
		extensions.add(".pytb");
		extensions.add(".r");
		extensions.add(".rhtml");
		extensions.add(".rkt");
		extensions.add(".raw");
		extensions.add(".rebol");
		extensions.add(".cw");
		extensions.add(".sass");
		extensions.add(".self");
		extensions.add(".sh");
		extensions.add(".st");
		extensions.add(".tpl");
		extensions.add(".sc");
		extensions.add(".tcl");
		extensions.add(".tcsh");
		extensions.add(".txt");
		extensions.add(".textile");
		extensions.add(".vhdl");
		extensions.add(".vala");
		extensions.add(".v");
		extensions.add(".vim");
		extensions.add(".bas");
		extensions.add(".yml");
		extensions.add(".mu");
		extensions.add(".ooc");
		extensions.add(".rst");
	}

	static final String MAYBE_ERROR_URL = "http://gist.github.com/gists/new";

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
			if (StringUtil.isEmpty(resultUri) == false) {
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
			nvs.add(new BasicNameValuePair("file_ext[gistfile1]",
					getExtension()));
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
				String location = h.getValue();
				if (MAYBE_ERROR_URL.equals(location)) {
					nvs.remove(1);// token
					LogUtil.log(Activator.getDefault(), nvs.toString());
				} else {
					result = location;
				}
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
		if (StringUtil.isEmpty(this.txt)) {
			return Streams.readText(this.file.getContents());
		} else {
			return this.txt;
		}
	}

	protected String getExtension() {
		String ext = this.file.getFileExtension();
		if (StringUtil.isEmpty(ext) == false) {
			ext = "." + ext.trim().toLowerCase();
			if (extensions.contains(ext)) {
				return ext;
			}
		}
		return ".txt";
	}

	protected String getLogin() {
		return Activator.getPreference(Constants.PREF_LOGIN);
	}

	protected String getToken() {
		return Activator.getPreference(Constants.PREF_TOKEN);
	}
}
