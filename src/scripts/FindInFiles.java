package scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.editors.text.UntitledTextFileWizard;
import org.eclipse.ui.texteditor.ITextEditor;

//TODO optimize resut view - don't show full path - only file name
//TODO jump to line,column not only to column
@SuppressWarnings("restriction")
public class FindInFiles {

	static String theDir = null;
	static IDocument doc = null;
	static String theFilter = null;

	public static void main(String[] args) {
		MyUtils.cleanup();
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				DirectoryDialog diag = new DirectoryDialog(shell);
				IEclipsePreferences node = InstanceScope.INSTANCE.getNode("org.eclipse.ease.ui");
				diag.setFilterPath(node.get("findLastDir", "c:/"));
				theDir = diag.open();
				if (theDir == null || theDir.isEmpty() == true) {
					return;
				}
				node.put("findLastDir", theDir);
				InputDialog id = new InputDialog(shell, "Find in files", "Specify filter [(?i:) = ignore case]:",
						"(?i:)", null);
				if (id.open() == Window.OK) {
					theFilter = id.getValue();
					UntitledTextFileWizard w = new UntitledTextFileWizard();
					w.init(PlatformUI.getWorkbench(), null);
					w.performFinish();
					ITextEditor ae = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
							.getActiveEditor();
					doc = ae.getDocumentProvider().getDocument(ae.getEditorInput());
					StyledText st = (StyledText) ae.getAdapter(Control.class);
					st.addVerifyKeyListener(new MyVerifyKeyListener());
					// MyRunnable(theDir,theFilter).run() # for debugging
					// purposes
					new Thread(new MyRunnable(theDir, theFilter)).start();
				}
			}
		});
	}

	static class InsertRunnable implements Runnable {
		private String path;
		private String fullPath;

		public InsertRunnable(String fullPath, String path) {
			this.fullPath = fullPath;
			this.path = path;
		}

		@Override
		public void run() {
			try {
				doc.replace(doc.getLength(), 0, path + "\n");
			} catch (BadLocationException e) {
				MyUtils.log(e);
			}
		}
	}

	private static boolean isTextFile(InputStream is) throws Exception {
		int size = is.available();
		if (size > 1000)
			size = 1000;
		byte[] data = new byte[size];
		is.read(data);
		is.close();
		String s = new String(data, "ISO-8859-1");
		String s2 = s.replaceAll("[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\"
				+ "+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîô" + "ÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]", "");
		// will delete all text signs

		double d = (double) (s.length() - s2.length()) / (double) (s.length());
		// percentage of text signs in the text
		return d > 0.95;
	}

	static boolean doStop = false;

	static void findInJar(File child, Pattern patt) {
		JarFile jf = null;
		try {
			jf = new JarFile(child);
			Enumeration<JarEntry> en = jf.entries();
			while (en.hasMoreElements() == true) {
				if (doStop == true) {
					return;
				}
				JarEntry entry = en.nextElement();
				InputStream ins = jf.getInputStream(entry);
				if (isTextFile(ins) == true) {
					ins.close();
					ins = jf.getInputStream(entry);
					String path = "jar:file:/" + child.getAbsolutePath() + "!/" + entry.getName();
					searchStringInStream(patt, path, ins);
					if (ins != null) {
						ins.close();
					}
				}
			}
		} catch (Exception e) {
			MyUtils.log(e);
		}
		if (jf != null) {
			try {
				jf.close();
			} catch (IOException e) {
				MyUtils.log(e);
			}
		}
	}

	static void findInFiles(File file, Pattern patt) {
		File[] children = file.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (doStop == true) {
				return;
			}
			if (child.isDirectory() == true) {
				findInFiles(child, patt);
				continue;
			}
			String nl = child.getName().toLowerCase(Locale.ENGLISH);
			if (nl.endsWith(".zip") == true || nl.endsWith(".jar") == true) {
				findInJar(child, patt);
				continue;
			}
			try {
				FileInputStream ins = new FileInputStream(child);
				if (isTextFile(ins) == true) {
					ins.close();
					ins = new FileInputStream(child);
					searchStringInStream(patt, child.getAbsolutePath(), ins);
					continue;
				}
				ins.close();
			} catch (Exception e) {
				MyUtils.log(e);
			}
		}
	}

	static void searchStringInStream(Pattern patt, String fullPath, InputStream ins) {
		if (fullPath.endsWith(".java") == false && fullPath.endsWith(".properties") == false
				&& fullPath.endsWith(".mf") == false && fullPath.endsWith(".xml") == false
				&& fullPath.endsWith(".html") == false && fullPath.endsWith(".js") == false) {
			return;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(ins));
		try {
			String line = null;
			int no = 0;
			while (true) {
				line = br.readLine();
				no = no + 1;
				if (line == null) {
					break;
				}
				Matcher matcher = patt.matcher(line);
				if (matcher.find() == true) {
					int ind = fullPath.lastIndexOf("/");
					int ind2 = fullPath.lastIndexOf("\\");
					if (ind2 > ind) {
						ind = ind2;
					}
					String name = fullPath.substring(ind + 1); // TODO: replace
																// fullpath with
																// name
					Display.getDefault().asyncExec(new InsertRunnable(fullPath, fullPath + ":" + no + ":" + line));
				}
			}
		} catch (Exception e) {
			MyUtils.log(e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				MyUtils.log(e);
			}
		}
	}

	static int getLine(String source, int offset) {
		String sub = source.substring(0, offset);
		int lineNumber = sub.length() - sub.replace("\n", "").length();
		return lineNumber + 1;
	}

	static String getLineMatch(String source, int start, int end) {
		String res = source.substring(start, end);
		return res;
	}

	static void onFinished() {
		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Finished", "Find in files finished");
	}

	static class MyRunnable implements Runnable {
		private String dir;
		private String filter;

		public MyRunnable(String dir, String filter) {
			this.dir = dir;
			this.filter = filter;
		}

		public void run() {
			findInFiles(new File(dir), Pattern.compile(filter));
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					onFinished();
				}
			});
		}
	}

	static void onFindResult() {
		try {
			ITextEditor ae = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			ISelection sel = ae.getSelectionProvider().getSelection();
			if (sel instanceof ITextSelection) {
				int off = ((ITextSelection) sel).getOffset();
				IDocument doc = ae.getDocumentProvider().getDocument(ae.getEditorInput());
				int line = doc.getLineOfOffset(off);
				int start = doc.getLineOffset(line);
				int len = doc.getLineLength(line);
				String lineString = doc.get(start, len);
				Pattern patt = Pattern.compile(":\\d+:");
				Matcher match = patt.matcher(lineString);
				if (match.find() == true) {
					int so = match.start();
					int eo = match.end();
					String path = lineString.substring(0, so);
					String no = lineString.substring(so + 1, eo - 1);
					if (path.startsWith("jar:") == true) {
						OpenFindResult.openJarFile(path);
					} else {
						LocalFile lf = new LocalFile(new File(path));
						IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
								lf);
					}
					// jump to line
					IEditorPart targetEditor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().getActiveEditor();
					if (targetEditor instanceof ITextEditor) {
						doc = ((ITextEditor) targetEditor).getDocumentProvider().getDocument(ae.getEditorInput());
						start = doc.getLineOffset(Integer.parseInt(no) - 1);
						ae.selectAndReveal(start, 0);
					}
				}
			}
		} catch (Exception e) {
			MyUtils.log(e);
		}
	}

	static class MyVerifyKeyListener implements VerifyKeyListener {
		public void verifyKey(org.eclipse.swt.events.VerifyEvent event) {
			if (event.character == SWT.CR) {
				event.doit = false;
				// trigger open file for given selection
				onFindResult();
			}
		}
	}
}
