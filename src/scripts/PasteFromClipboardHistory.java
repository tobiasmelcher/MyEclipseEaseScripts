package scripts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class PasteFromClipboardHistory implements IHandler{

	private static void insert(Object res) {
		IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ITextEditor te = getTextEditor(ed);
		if (te!=null) {
			StyledText st = (StyledText) te.getAdapter(Control.class);
			String str = res.toString();
			int idx = str.indexOf(":");
			str = str.substring(idx+1);
			st.insert(str);
		}
	}

	private static ITextEditor getTextEditor(IEditorPart ed) {
		if (ed instanceof ITextEditor) {
			return (ITextEditor) ed;
		}else if (ed instanceof MultiPageEditorPart) {
			Object sp = ((MultiPageEditorPart) ed).getSelectedPage();
			if (sp instanceof ITextEditor) {
				return (ITextEditor) sp;
			}
		}
		return null;
	}

	public static void registerCopyListener() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				ICommandService commandService = workbench.getService(ICommandService.class);
				Command cmd = commandService.getCommand("org.eclipse.ui.edit.copy");
				cmd.addExecutionListener(new IExecutionListener() {

					@Override
					public void preExecute(String commandId, ExecutionEvent event) {

					}

					@Override
					public void postExecuteSuccess(String commandId, Object returnValue) {
						Display def = Display.getDefault();
						Clipboard cb = new Clipboard(def);
						try {
							Object contents = cb.getContents(TextTransfer.getInstance());
							@SuppressWarnings("unchecked")
							List<String> data = (List<String>) def.getData("my_history");
							if (data == null) {
								data = new ArrayList<>();
								def.setData("my_history", data);
							}
							data.add(contents.toString());
							if (data.size()>50) {
								data.remove(0);
							}
						} finally {
							cb.dispose();
						}
					}

					@Override
					public void postExecuteFailure(String commandId, ExecutionException exception) {
					}

					@Override
					public void notHandled(String commandId, NotHandledException exception) {
					}
				});
			}
		});
	}

	public void addKeyHandlerAndRegisterCopyListener() {
		PasteFromClipboardHistory.registerCopyListener(); // for clipboard history
		MyUtils.addKeyHandler(this, "pasteFromClipbardHistory", "CTRL+SHIFT+V");
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MyUtils.cleanup();
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				final ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getDefault()
						.getActiveShell(), new LabelProvider());
				dialog.setAllowDuplicates(true);
				dialog.setTitle("Paste");
				dialog.setMessage("Paste from clipboard history");
				@SuppressWarnings("unchecked")
				final List<String> history = (List<String>) Display.getDefault().getData("my_history");
				Object[] els = new Object[history.size()];
				for (int i=0;i<els.length;i++) {
					els[i]=(100+els.length-i)+":"+history.get(i);
				}
				dialog.setElements(els);
				dialog.open();
				Object res = dialog.getFirstResult();
				if (res != null) {
					insert(res);
				}
			}
		});
		return null;
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
