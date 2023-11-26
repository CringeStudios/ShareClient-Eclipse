package me.mrletsplay.shareclient.handlers;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.WebSocketConnection;
import me.mrletsplay.shareclientcore.document.DocumentListener;
import me.mrletsplay.shareclientcore.document.SharedDocument;

public class SampleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				"ShareClient",
				"Hello, Eclipse world");

		IEditorPart editor = window.getActivePage().getActiveEditor();
		if(!(editor instanceof ITextEditor)) return null;

		ITextEditor textEditor = (ITextEditor) editor;

		IDocument eclipseDocument = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

		RemoteConnection con = new WebSocketConnection(URI.create("ws://localhost:5473"));
		try {
			con.connect();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		AtomicBoolean ignoreChanges = new AtomicBoolean(false);
		SharedDocument doc = new SharedDocument(con);
		doc.localInsert(0, eclipseDocument.get());

		doc.addListener(new DocumentListener() {

			@Override
			public void onInsert(int index, char character) {
				Display.getDefault().asyncExec(() -> {
					try {
						ignoreChanges.set(true);
						eclipseDocument.replace(index, 0, String.valueOf(character));
						ignoreChanges.set(false);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				});

			}

			@Override
			public void onDelete(int index) {
				Display.getDefault().asyncExec(() -> {
					try {
						ignoreChanges.set(true);
						eclipseDocument.replace(index, 1, "");
						ignoreChanges.set(false);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				});
			}
		});

		eclipseDocument.addDocumentListener(new IDocumentListener() {

			@Override
			public void documentChanged(DocumentEvent event) {
				if(ignoreChanges.get()) return; // TODO: not very ideal

				if(event.getLength() > 0) {
					doc.localDelete(event.getOffset(), event.getLength());
				}

				doc.localInsert(event.getOffset(), event.getText());
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {

			}
		});

		return null;
	}
}
