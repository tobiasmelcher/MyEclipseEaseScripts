package scripts;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.internal.registry.ExtensionRegistry;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.EditorDescriptor;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.part.EditorPart;

@SuppressWarnings({ "restriction", "deprecation" })
public class EditorUtil {

	public static interface EditorFactory {
		public EditorPart createEditor();
	}
	
	public static void registerEditor(String fileExtension,final EditorFactory factory) {
		EditorRegistry reg = (EditorRegistry) WorkbenchPlugin.getDefault()
				.getEditorRegistry();
		try {
			Constructor<EditorDescriptor> c = EditorDescriptor.class.getDeclaredConstructor(String.class,IConfigurationElement.class);
			c.setAccessible(true);
			EditorDescriptor editor=(EditorDescriptor) c.newInstance("editordescriptorid", new IConfigurationElement() {
				
				@Override
				public boolean isValid() {
					return false;
				}
				
				@Override
				public String getValueAsIs() throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String getValue(String locale) throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String getValue() throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public Object getParent() throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String getNamespaceIdentifier()
						throws InvalidRegistryObjectException {
					return "esp";
				}
				
				@Override
				public String getNamespace() throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String getName() throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public IExtension getDeclaringExtension()
						throws InvalidRegistryObjectException {
					return createExtension();
				}
				
				@Override
				public IContributor getContributor() throws InvalidRegistryObjectException {
					return EditorUtil.getContributor();
				}
				
				@Override
				public IConfigurationElement[] getChildren(String name)
						throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public IConfigurationElement[] getChildren()
						throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String[] getAttributeNames() throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String getAttributeAsIs(String name)
						throws InvalidRegistryObjectException {
					return null;
				}
				
				@Override
				public String getAttribute(String attrName, String locale)
						throws InvalidRegistryObjectException {
					return getAttribute(attrName);
				}
				
				@Override
				public String getAttribute(String name)
						throws InvalidRegistryObjectException {
					if (IWorkbenchRegistryConstants.ATT_LAUNCHER.equals(name) || IWorkbenchRegistryConstants.ATT_COMMAND.equals(name)){
						return null;
					}
					return "";
				}
				
				@Override
				public Object createExecutableExtension(String propertyName)
						throws CoreException {
					if ("class".equals(propertyName)) {
						return factory.createEditor();
					}else if ("contributorClass".equals(propertyName)) {
						return new TextEditorActionContributor();
					}
					return null;
				}
			});
			reg.addEditorFromPlugin(editor, Arrays.asList(fileExtension), new ArrayList<>(), new ArrayList<>(), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static IExtension createExtension() {
		return new IExtension() {
			
			@Override
			public boolean isValid() {
				return false;
			}
			
			@Override
			public String getUniqueIdentifier() throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public String getSimpleIdentifier() throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public String getNamespaceIdentifier()
					throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public String getNamespace() throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public String getLabel(String locale) throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public String getLabel() throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public String getExtensionPointUniqueIdentifier()
					throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public IPluginDescriptor getDeclaringPluginDescriptor()
					throws InvalidRegistryObjectException {
				return null;
			}
			
			@Override
			public IContributor getContributor() throws InvalidRegistryObjectException {
				return EditorUtil.getContributor();
			}
			
			@Override
			public IConfigurationElement[] getConfigurationElements()
					throws InvalidRegistryObjectException {
				return null;
			}
		};
	}
	
	private static IContributor getContributor() {
		ExtensionRegistry registry = (ExtensionRegistry) Platform.getExtensionRegistry();
		IContributor[] allContributors = registry.getAllContributors();
		for (IContributor c:allContributors) {
			if ("esp".equals(c.getName())) {
				return c;
			}
		}
		return null;
	}
}
