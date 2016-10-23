package scripts;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class MoveSelectedTextToEnd {
	
	public static void main(String[] args) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				IEditorPart ae  = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				if (ae instanceof ITextEditor) {
					ITextEditor te = (ITextEditor) ae;
					ITextSelection ts = (ITextSelection) te.getSelectionProvider().getSelection();
					IDocument document = te.getDocumentProvider().getDocument(te.getEditorInput());
					try {
						String sub = document.get(ts.getOffset(), ts.getLength());
						document.replace(ts.getOffset(), ts.getLength(), "");
						document.replace(document.getLength(), 0, sub);
					} catch (BadLocationException e) {
						MyUtils.log(e);
					}
				}
			}
		});
	}
}
