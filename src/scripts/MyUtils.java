package scripts;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;
import org.osgi.framework.Bundle;

@SuppressWarnings("restriction")
public class MyUtils {

	/**
	 * EASE is not removing the launch (unnecessary entry in Debug view) and the
	 * script console after running a script. Running the same script a second
	 * time can then be very irritating. This cleanup method removes all script
	 * launches and terminated consoles from previous runs.
	 */
	public static void cleanup() {
		removeAllLaunches();
		removeAllTerminatedConsoles();
	}

	public static void removeAllTerminatedConsoles() {
		try {
			IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
			List<IConsole> candidates = new ArrayList<>();
			for (IConsole console : consoles) {
				if (console.getClass().getName().contains("ScriptConsole")) {
					Method m = console.getClass().getDeclaredMethod("getScriptEngine");
					m.setAccessible(true);
					Object se = m.invoke(console);
					if (se == null) {
						candidates.add(console);
					}
				}
			}
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(candidates.toArray(new IConsole[] {}));
		} catch (Exception e) {
			log(e.getMessage());
		}
	}

	public static void removeAllLaunches() {
		ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] ls = lm.getLaunches();
		for (ILaunch l : ls) {
			try {
				String tn = l.getLaunchConfiguration().getType().getName();
				if (tn != null && tn.equalsIgnoreCase("EASE Script")) {
					lm.removeLaunch(l);
				}
			} catch (CoreException e) {
				log(e.getMessage());
			}
		}
	}

	public static void log(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String s = sw.toString();
		log(s);
	}

	/**
	 * log string to EASE script console
	 * 
	 * @param str
	 */
	public static void log(String str) {
		try {
			IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
			for (IConsole cons : consoles) {
				if (cons.getClass().getName().contains("ScriptConsole")) {
					Method m = cons.getClass().getDeclaredMethod("getOutputStream");
					m.setAccessible(true);
					OutputStream os = (OutputStream) m.invoke(cons);
					os.write(str.getBytes("UTF-8"));
					os.write("\n".getBytes("UTF-8"));
					os.flush();
					return;
				}
			}
		} catch (Exception e) {
			log(e);
		}
	}

	/**
	 * registers an eclipse keyboard handler with given commandId and
	 * keySequenceString
	 * 
	 * @param handler
	 * @param commandId
	 * @param keySequenceString
	 */
	public static void addKeyHandler(IHandler handler, String commandId, String keySequenceString) {
		try {
			String shortId = "ddlpp";
			String ext = "<plugin> " + //
					" <extension " + //
					"   point=\"org.eclipse.ui.commands\"> " + //
					"<command " + //
					"   categoryId=\"org.eclipse.ui.category.textEditor\" " + //
					"    defaultHandler=\"my_scripts.TestAddExtension\" " + //
					"  id=\"" + //
					commandId + //
					"\" " + //
					" name=\"" + //
					commandId + "\"> " + //
					"</command> " + //
					"</extension> " + //
					"</plugin>";
			// cannot access org.eclipse.core.runtime.Platform class -
			// reflection needed
			// espBundle =
			// org.eclipse.core.runtime.Platform.getBundle("org.eclipse.ease.ui")
			IWorkbench wb = PlatformUI.getWorkbench();
			ClassLoader cl = wb.getClass().getClassLoader();
			Class<?> cls = cl.loadClass("org.eclipse.core.runtime.Platform");
			Method m = cls.getDeclaredMethod("getBundle", String.class);
			Object espBundle = m.invoke(cls, "org.eclipse.ease.ui");

			m = cls.getDeclaredMethod("getExtensionRegistry");
			ExtensionRegistry extensionRegistry = (ExtensionRegistry) m.invoke(cls);

			// same with ContributorFactoryOSGi class - reflection needed
			// contributor = ContributorFactoryOSGi.createContributor(espBundle)
			cls = cl.loadClass("org.eclipse.core.runtime.ContributorFactoryOSGi");
			m = cls.getDeclaredMethod("createContributor", Bundle.class);
			IContributor contributor = (IContributor) m.invoke(cls, espBundle);

			extensionRegistry.addContribution(new ByteArrayInputStream(ext.getBytes()), contributor, false, shortId,
					null, extensionRegistry.getTemporaryUserToken());
			IWorkbench workbench = PlatformUI.getWorkbench();
			ICommandService commandService = workbench.getService(ICommandService.class);
			Command c = commandService.getCommand(commandId);
			ParameterizedCommand pc = new ParameterizedCommand(c, null);
			if (c.isDefined() == false) {
				Category cat = commandService.getCategory("org.eclipse.ui.category.textEditor");
				c.define("a", "b", cat);
			}
			log(c.toString());
			c.setHandler(handler);
			IHandlerService handlerService = workbench.getService(IHandlerService.class);
			handlerService.activateHandler(commandId, c.getHandler());
			// register key binding
			BindingService bindingService = (BindingService) workbench.getAdapter(IBindingService.class);
			bindingService.addBinding(new KeyBinding(KeySequence.getInstance(keySequenceString), pc,
					"org.eclipse.ui.defaultAcceleratorConfiguration", "org.eclipse.ui.contexts.dialogAndWindow", null,
					null, null, 0));
		} catch (Exception e) {
			log(e);
		}
	}
}
