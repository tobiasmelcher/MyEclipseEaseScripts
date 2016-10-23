package scripts;

import java.util.Arrays;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class OpenUrlInChrome implements IHandler {

	public void addKeyHandler() {
		MyUtils.addKeyHandler(this, "openInChrome", "ALT+Y");
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}
	
	public static void main(String[] args) {
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				try {
					new OpenUrlInChrome().execute(null);
				} catch (ExecutionException e) {
					MyUtils.log(e);
				}
			}
		});
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		MessageDialog.openInformation(Display.getDefault().getActiveShell(), "aa", "bbb");
		IEditorPart ae = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (ae instanceof ITextEditor) {
			ITextEditor te = (ITextEditor) ae;
			ISelection sel = te.getSelectionProvider().getSelection();
			if (sel instanceof ITextSelection) {
				ITextSelection ts = (ITextSelection) sel;
				int offset = ts.getOffset();
				int len = ts.getLength();
				IDocument doc = te.getDocumentProvider().getDocument(te.getEditorInput());
				try {
					int start = 0;
					int end = 0;
					if (len > 0) {
						start = offset;
						end = offset + len;
					} else {
						start = getLeftOffset(doc, offset);
						end = getRightOffset(doc, offset);
					}
					String url = doc.get(start, end - start).trim();
					if (len == 0 && url.startsWith("http") == false && url.startsWith("www") == false
							&& url.startsWith("file:") == false) {
						start = getLeftBackslash(doc, offset);
						url = doc.get(start, end - start).trim();
					}
					if (url.startsWith("\\\\")) {
						MyUtils.log(url);
						final String fUrl = url;
						new Thread(new Runnable() {

							@Override
							public void run() {
								CommandUtil.run(Arrays.asList(new String[] { "explorer.exe", fUrl }), null);
							}
						}).start();
					} else {
						// open url in chrome
						CommandUtil.run(
								Arrays.asList(new String[] {
										"c:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe", url }),
								null);
					}
				} catch (BadLocationException e) {
				}
			}
		}
		return null;
	}

	private int getLeftBackslash(IDocument doc, int offset) throws BadLocationException {
		while (true) {
			char ch = doc.getChar(offset);
			if (ch == '\\') {
				char prev = doc.getChar(offset - 1);
				if (prev == '\\') {
					return offset - 1;
				}
			}
			offset--;
			if (offset < 0)
				return offset;
		}
	}

	private int getLeftOffset(IDocument doc, int offset) throws BadLocationException {
		while (true) {
			char ch = doc.getChar(offset);
			if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
				return offset + 1;
			offset--;
			if (offset < 0)
				return 0;
		}
	}

	private int getRightOffset(IDocument doc, int offset) throws BadLocationException {
		while (true) {
			char ch = doc.getChar(offset);
			if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')
				return offset;
			offset++;
			if (offset >= doc.getLength())
				return doc.getLength() - 1;
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
}
