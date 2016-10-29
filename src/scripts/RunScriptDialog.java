package scripts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * this script registers a keyboard handler (ALT-X) which brings up a dialog of
 * all executable java files in the eclipse project "p1"
 * 
 * Run this script once when eclipse is started and you can access all your
 * other scripts via ALT-X
 */
public class RunScriptDialog {

	public static void main(String[] args) {
		MyUtils.cleanup();
		MyUtils.addKeyHandler(new IHandler() {

			@Override
			public void removeHandlerListener(IHandlerListener handlerListener) {
			}

			@Override
			public boolean isHandled() {
				return true;
			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {
				ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getDefault()
						.getActiveShell(), new LabelProvider());
				dialog.setTitle("Run script");
				dialog.setMessage("Select a script (* = any string, ? = any char):");
				dialog.setElements(getAllJavaEaseScripts());
				dialog.open();
				Object res = dialog.getFirstResult();
				if (res != null) {
					runScript(res);
				}
				return null;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addHandlerListener(IHandlerListener handlerListener) {
			}
		}, "run_script_handler", "ALT+X");
		new OpenUrlInChrome().addKeyHandler(); // register ALT-Y key 
		new PasteFromClipboardHistory().addKeyHandlerAndRegisterCopyListener(); // register CTRL-SHIFT-V
	}

	private static void runScript(Object res) {
		try {
			IFile f = (IFile) res;
			MyUtils.log(f.toString());
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
			for (ILaunchConfiguration config : configs) {
				String n = config.getType().getName();
				if (n != null && n.equalsIgnoreCase("EASE Script")) {
					// ks = x.getAttributes().keySet()
					// consoleOut(str(ks))
					// loc = x.getAttribute("File location",None)
					// consoleOut(str(loc))
					String location = "workspace:/" + f.getFullPath().toString();
					ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
					copy.setAttribute("File location", location);
					copy.launch(ILaunchManager.RUN_MODE, null);
					return;
				}
			}
			MyUtils.log("launch config not found");
		} catch (Exception e) {
			MyUtils.log(e.getMessage());
		}
	}

	private static Object[] getAllJavaEaseScripts() {
		final List<Object> result = new ArrayList<>();
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("my_scripts");
		try {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile f = (IFile) resource;
						String ext = f.getFileExtension();
						if ("java".equalsIgnoreCase(ext)) {
							result.add(f);
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			MyUtils.log(e.getMessage());
		}
		return result.toArray();
	}
}
