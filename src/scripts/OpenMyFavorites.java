package scripts;

import java.io.File;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

@SuppressWarnings("restriction")
public class OpenMyFavorites {
	public static void main(String[] args) {
		MyUtils.cleanup();
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					String p = "C:\\wbem\\jedt_self_contained\\eclipse_mars\\runtime-all\\my_scripts\\src\\my_scripts\\org_mode\\myfavorites.txt";
					LocalFile lf = new LocalFile(new File(p));
					IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), lf);
				} catch (PartInitException e) {
					MyUtils.log(e);
				}
			}
		});
	}
}
