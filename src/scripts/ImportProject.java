package scripts;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

@SuppressWarnings("restriction")
public class ImportProject {

	private static class FindProjectsRunnable implements Runnable {
		private String theDir;

		public FindProjectsRunnable(String theDir) {
			this.theDir = theDir;
		}

		@Override
		public void run() {
			findAllProjectFiles(new File(theDir));
			refreshDialog();
		}

		private void refreshDialog() {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					onRefreshDialog();
				}
			});
		}
	}

	static List<Object> projects = new ArrayList<>();
	static ElementListSelectionDialog dialog;

	private static void onRefreshDialog() {
		if (dialog.getShell() == null) {
			return;
		}
		for (Method x : AbstractElementListSelectionDialog.class.getDeclaredMethods()) {
			if (x.getName().equals("setListElements")) {
				try {
					x.setAccessible(true);
					Object[] l = projects.toArray();
					dialog.setElements(l);
					Object[] param = { l };
					x.invoke(dialog, param);
				} catch (Exception e) {
					MyUtils.log(e);
				}
			}
		}
	}

	private static void findAllProjectFiles(File file) {
		File[] files = file.listFiles();
		if (files != null) {
			for (File x : files) {
				String name = x.getName();
				if (name.equalsIgnoreCase(".project") == true) {
					projects.add(x);
					return;
				}
				findAllProjectFiles(x);
			}
		}
	}

	public static void main(String[] args) {
		MyUtils.cleanup();
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				DirectoryDialog diag = new DirectoryDialog(Display.getDefault().getActiveShell());
				// store "findLastDir" in preferences
				IEclipsePreferences node = InstanceScope.INSTANCE.getNode("org.eclipse.ease.ui");
				diag.setFilterPath(node.get("importProjectLastDir", "c:/"));
				String theDir = diag.open();
				if (theDir==null || theDir.length()==0) {
					return;
				}
				node.put("importProjectLastDir", theDir);
				Thread t = new Thread(new FindProjectsRunnable(theDir));
				t.start();
				dialog = new ElementListSelectionDialog(Display.getDefault().getActiveShell(), new LabelProvider());
				dialog.setTitle("Select project to import");
				dialog.setMessage("Select a project (* = any string, ? = any char):");
				projects.clear();
				projects.add("a");
				dialog.setElements(projects.toArray());
				dialog.open();
				Object res = dialog.getFirstResult();
				if (res != null) {
					importProject(res);
				}
			}
		});
	}

	private static void importProject(Object res) {
		try {
			File f = (File) res;
			String path = f.getAbsolutePath();
			IProjectDescription description = IDEWorkbenchPlugin.getPluginWorkspace().loadProjectDescription(
					new Path(path));
			String projectName = description.getName();
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (project.exists() == true && project.isOpen() == false) {
				project.open(IResource.BACKGROUND_REFRESH, new NullProgressMonitor());
			} else {
				project.create(description, new NullProgressMonitor());
				project.open(IResource.BACKGROUND_REFRESH, new NullProgressMonitor());
			}
		} catch (Exception e) {
			MyUtils.log(e);
		}
	}
}
