package scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
public class OpenFindResult {

	public void onMainOpenFindResult() {
		try {
			ITextEditor ae = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.getActiveEditor();
			ISelection sel = ae.getSelectionProvider().getSelection();
			if (sel instanceof ITextSelection) {
				int off = ((ITextSelection) sel).getOffset();
				IDocument doc = ae.getDocumentProvider().getDocument(ae.getEditorInput());
				int line = doc.getLineOfOffset(off);
				int start = doc.getLineOffset(line);
				int len = doc.getLineLength(line);
				String lineString = doc.get(start, len);
				File f = new File(lineString.trim());
				if (f.isDirectory() == true) {
					// TODO: open directory
					return;
				} else { // aha is file
					if (f.exists() == false) {
						if (lineString.startsWith("jar:file:") == true) {
							openJarFile(lineString);
						}
						return;
					}
					LocalFile lf = new LocalFile(f);
					IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), lf);
				}
			}
		} catch (Exception e) {
			MyUtils.log(e);
		}
	}

	private static String readFile(InputStream stream) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			StringBuilder stringBuilder = new StringBuilder();
			String ls = System.getProperty("line.separator");
			try {
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append(ls);
				}
				return stringBuilder.toString();
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			MyUtils.log(e);
		}
		return null;
	}

	public static void openJarFile(String str) {
		try {
			URL u = new URL(str);
			InputStream stream = u.openStream();
			String content = readFile(stream);
			stream.close();
			String tempDir = System.getProperty("java.io.tmpdir") + "/espPlugin";
			File dir = new File(tempDir);
			if (dir.exists() == false) {
				dir.mkdir();
			}
			dir.deleteOnExit();
			int ind = str.lastIndexOf("/");
			if (ind < 0) {
				ind = str.lastIndexOf("\\");
			}
			String name = str.substring(ind + 1).trim();
			File f = new File(tempDir + "/" + name);
			if (f.exists() == false) {
				f.createNewFile();
			}
			f.deleteOnExit();
			FileWriter w = new FileWriter(f);
			w.write(content);
			w.close();
			LocalFile lf = new LocalFile(f);
			IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), lf);
		} catch (Exception e) {
			MyUtils.log(e);
		}
	}
}
