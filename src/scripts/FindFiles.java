package scripts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.text.UntitledTextFileWizard;
import org.eclipse.ui.internal.ide.StringMatcher;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * find files inside given directory; searches also for files inside zip and jar files.
 *
 */
@SuppressWarnings("restriction")
public class FindFiles {
	static String theFilter = null;
	static String theDir = null;
	static IDocument doc = null;
	static StyledText st=null;

	public static void main(String[] args) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				DirectoryDialog diag = new DirectoryDialog(shell);
				// store "findLastDir" in preferences
				IEclipsePreferences node = InstanceScope.INSTANCE.getNode("org.eclipse.ease.ui");
				diag.setFilterPath(node.get("findLastDir", "c:/"));
				theDir = diag.open();
				InputDialog id = new InputDialog(shell, "Filter Text", "Specify filter (*,?):", "", null);
				if (id.open() == Window.OK) {
					theFilter = id.getValue();
				}
				if (theFilter == null || theFilter.isEmpty()) {
					return;
				}
				node.put("findLastDir", theDir);
				UntitledTextFileWizard w = new UntitledTextFileWizard();
				w.init(PlatformUI.getWorkbench(), null);
				w.performFinish();
				ITextEditor ae = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.getActiveEditor();
				doc = ae.getDocumentProvider().getDocument(ae.getEditorInput());
				st = (StyledText) ae.getAdapter(Control.class);
				st.addVerifyKeyListener(new MyVerifyKeyListener());
				new Thread(new Runnable() {

					@Override
					public void run() {
						runFind();
					}
				}).start();
			}
		});
	}

	private static class MyVerifyKeyListener implements VerifyKeyListener {

		@Override
		public void verifyKey(VerifyEvent event) {
			if (event.character == SWT.CR) {
				event.doit = false;
				//  trigger open file for given selection
				new OpenFindResult().onMainOpenFindResult();
			}
		}
	}

	private static void runFind() {
		StringMatcher matcher = new StringMatcher(theFilter, true, false);
		findFiles(matcher, new File(theDir), true);
	}

	private static void findFiles(StringMatcher matcher, File f, boolean searchInJarFiles) {
		if (f == null) {
			return;
		}
		String name = f.getName();
		String nameInLower = name.toLowerCase(Locale.ENGLISH);
		if (searchInJarFiles == true && (nameInLower.endsWith(".zip") || nameInLower.endsWith(".jar"))) {
			findFilesInJar(matcher, f, searchInJarFiles);
		}
		if (matcher.match(name)) {
			String pa = f.getAbsolutePath();
			Display.getDefault().asyncExec(new MyRunnable(pa));
		}
		File[] files = f.listFiles();
		if (files == null) {
			return;
		}
		for (File child : files) {
			findFiles(matcher, child, searchInJarFiles);
		}
	}

	private static void findFilesInJar(StringMatcher matcher, File f, boolean searchInJarFiles) {
		try {
			JarFile jf = new JarFile(f);
			Enumeration<JarEntry> en = jf.entries();
			while (en.hasMoreElements()) {
				JarEntry entry = en.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String n = entry.getName();
				String name = n;
				int ind = n.lastIndexOf("/");
				if (ind > 0) {
					name = name.substring(ind + 1);
				}
				if (matcher.match(name)) {
					String path = "jar:file:/" + f.getAbsolutePath() + "!/" + n;
					Display.getDefault().asyncExec(new MyRunnable(path));
				}
			}
			if (jf != null) {
				jf.close();
			}
		} catch (IOException e) {
			MyUtils.log(e);
		}
	}
	
	private static Runnable colorizeRunnable =new Runnable() {
		@Override
		public void run() {
			// mark file name at end of path as bold
			List<StyleRange> ranges = new ArrayList<>();
			String source = st.getText();
			Color black = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			Matcher matcher = Pattern.compile("([a-zA-Z0-9]+\\.[a-zA-Z0-9]+)(\\r)?\\n").matcher(source);
			while (matcher.find()) {
				int start = matcher.start(1);
				int end = matcher.end(1);
				ranges.add(new StyleRange(start, end-start, black, null, SWT.BOLD));
			}
			st.setStyleRanges(ranges.toArray(new StyleRange[] {}));			
		}
	};

	private static class MyRunnable implements Runnable {
		private String pa;

		public MyRunnable(String pa) {
			this.pa = pa;
		}

		@Override
		public void run() {
			try {
				doc.replace(doc.getLength(), 0, pa + "\n");
				Display.getDefault().timerExec(1000, colorizeRunnable);
			} catch (BadLocationException e) {
				MyUtils.log(e);
			}
		}
	}
}
