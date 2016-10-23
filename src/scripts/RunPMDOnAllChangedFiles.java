package scripts;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

/**
 * finds GIT repo for current open editor, runs then "git status" and PMD checks for all modified files
 */
public class RunPMDOnAllChangedFiles {

	public static void main(String[] args) {
		MyUtils.cleanup();
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				IEditorPart ae = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				IEditorInput ei = ae.getEditorInput();
				if (ei instanceof IFileEditorInput) {
					IFile f = ((IFileEditorInput) ei).getFile();
					String fp = f.getLocation().toOSString();
					String gitRoot = getGitRootFolder(fp);
					if (gitRoot == null) {
						return;
					}
					List<String> files = getModifiedFiles(gitRoot);
					runPmd(files);
				}
			}
		});
	}

	private static List<String> getModifiedFiles(String gitRoot) {
		List<String> result = new ArrayList<>();
		String res = CommandUtil.run(Arrays.asList("c:\\Program Files (x86)\\Git\\bin\\git.exe", "status"), new File(
				gitRoot));
		Matcher m = Pattern.compile("modified:([^\\r\\n]*)").matcher(res);
		while (m.find()) {
			String line = m.group(1).trim();
			result.add(gitRoot + "/" + line);
		}
		return result;
	}

	private static String getGitRootFolder(String path) {
		File f = new File(path + "/.git");
		if (f.exists()) {
			return path;
		}
		File pf = new File(path).getParentFile();
		if (pf == null || pf.exists() == false) {
			return null;
		}
		return getGitRootFolder(pf.getAbsolutePath());
	}

	private static void runPmd(List<String> allFiles) {
		try {
			for (String file : allFiles) {
				@SuppressWarnings("deprecation")
				IFile[] r = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(file));
				if (r != null) {
					for (IFile c : r) {
						if (c instanceof IFile) {
							IFile f = (IFile) c;
							runPmdCheckAction(f);
						}
					}
				}
			}
		} catch (Throwable e1) {
			MyUtils.log(e1);
		}
	}

	private static Method reviewSingleResourceMethod;
	private static Object pmdCheckAction;

	private static void runPmdCheckAction(IFile f) {
		try {
			if (pmdCheckAction == null) {
				Bundle bundle = Platform.getBundle("net.sourceforge.pmd.eclipse.plugin");
				if (bundle.getSymbolicName().contains(".pmd")) {
					Class<?> clazz = bundle.loadClass("net.sourceforge.pmd.eclipse.ui.actions.PMDCheckAction");
					pmdCheckAction = clazz.newInstance();
					reviewSingleResourceMethod = clazz.getDeclaredMethod("reviewSingleResource", IResource.class);
					reviewSingleResourceMethod.setAccessible(true);
				}
			}
			reviewSingleResourceMethod.invoke(pmdCheckAction, f);
		} catch (Throwable e1) {
			MyUtils.log(e1);
		}
	}
}
