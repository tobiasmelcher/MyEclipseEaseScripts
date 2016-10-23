package scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
public class OpenTodos {
	public static void main(String[] args) {
		MyUtils.cleanup();
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					String p = "c:\\wbem\\jedt_self_contained\\eclipse_mars\\runtime-all\\my_scripts\\src\\my_scripts\\org_mode\\TODOs.org";
					LocalFile lf = new LocalFile(new File(p));
					ITextEditor te = (ITextEditor) IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage(), lf);
					final RefreshRunnable r = new RefreshRunnable(te);
					IDocument doc = te.getDocumentProvider().getDocument(te.getEditorInput());
					doc.addDocumentListener(new IDocumentListener() {
						@Override
						public void documentChanged(DocumentEvent event) {
							Display.getDefault().timerExec(1000, r);
						}
						@Override
						public void documentAboutToBeChanged(DocumentEvent event) {
						}
					});
					te.addPropertyListener(new IPropertyListener() {
						@Override
						public void propertyChanged(Object source, int propId) {
							if (IEditorPart.PROP_DIRTY == propId) {
								Display.getDefault().timerExec(1000,r);
							}
						}
					});
					Display.getDefault().timerExec(1000, r);
				} catch (PartInitException e) {
					MyUtils.log(e);
				}
			}
		});
	}

	private static class RefreshRunnable implements Runnable {
		private ITextEditor ed;

		public RefreshRunnable(ITextEditor ed) {
			this.ed = ed;
		}

		public void run() {
			colorize(ed);
		}
	}

	private static void colorize(IEditorPart ep) {
		try {
			ITextEditor te = (ITextEditor) ep;
			StyledText st = (StyledText) te.getAdapter(Control.class);
			Color blue = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
			Color green = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
			List<StyleRange> ranges = new ArrayList<>();
			String source = st.getText();
			int offset = 0;
			while (true) {
				int end = source.indexOf("\n", offset);
				if (end < 0)
					break;
				String line = source.substring(offset, end).trim().toLowerCase(Locale.ENGLISH);
				if (line.startsWith("* todo") || line.contains("todo ")) {
					ranges.add(new StyleRange(offset, line.length(), blue, null, SWT.BOLD));
				} else if (line.startsWith("* done") || line.contains("done ")) {
					ranges.add(new StyleRange(offset, line.length(), green, null, SWT.BOLD));
				}
				offset = end + 1;
			}
			st.setStyleRanges(ranges.toArray(new StyleRange[] {}));
		} catch (Exception e) {
			MyUtils.log(e);
		}
	}
}
