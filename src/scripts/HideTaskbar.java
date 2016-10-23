package scripts;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * simply hide the taskbar of the eclipse window (tested on mars)
 */
public class HideTaskbar {
	public static void main(String[] args) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				Control taskbar = findTaskbar(shell.getChildren());
				taskbar.setVisible(false);
				shell.layout();
			}
		});
	}

	private static Control findTaskbar(Control[] children) {
		Control res = null;
		int max = 0;
		for (Control child : children) {
			int count = getStatusLineNumber((Composite) child);
			if (count > max) {
				max = count;
				res = child;
			}
		}
		return res;
	}

	private static int getStatusLineNumber(Composite control) {
		int count = 0;
		for (Control child : control.getChildren()) {
			String n = child.getClass().getName();
			if (n.contains("StatusLine")) {
				count++;
			}
		}
		return count;
	}
}
