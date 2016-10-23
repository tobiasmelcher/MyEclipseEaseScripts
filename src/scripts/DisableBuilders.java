package scripts;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.events.InternalBuilder;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

@SuppressWarnings("restriction")
public class DisableBuilders {

	/**
	 * disables all auto builders except the JavaBuilder.
	 * 
	 * This helps reduce build times if your workspace is huge and slow builders
	 * are configured (like PMD).
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		MyUtils.cleanup();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (project.isAccessible()) {
				ICommand[] commands = ((Project) project).internalGetDescription().getBuildSpec(false);
				for (ICommand command : commands) {
					if (command instanceof BuildCommand) {
						BuildCommand bc = (BuildCommand) command;
						String name = bc.getName();
						if ("com.sap.adt.pde.ManifestBuilder".equals(name))
							continue;
						if ("org.eclipse.jdt.core.javabuilder".equals(name)) {
							continue;
						}
						// not the java builder and the adt builder; disable it now
						bc.setBuilders(new NullBuilder());
					}
				}
			}
		}
	}

	private static class NullBuilder extends IncrementalProjectBuilder {
		public NullBuilder() {
			try {
				Field f = InternalBuilder.class.getDeclaredField("command");
				f.setAccessible(true);
				f.set(this, new BuildCommand());
			} catch (Exception e) {
				MyUtils.log(e);
			}
		}

		@Override
		protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
			return null;
		}
	}
}
