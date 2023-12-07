package me.mrletsplay.shareclient.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import me.mrletsplay.shareclient.ShareClient;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;

public class ShareProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		if(selection.isEmpty()) return null;

		IResource res = Adapters.adapt(selection.getFirstElement(), IResource.class, false);
		if(res == null) return null;

		IProject project = res.getProject();
		if(project == null) return null;

		IPath path = project.getLocation();
		if(path == null) return null;

		RemoteConnection con = ShareClient.getDefault().getOrOpenConnection();
		if(con == null) return null;

		// TODO: handle case when adding project to existing session

//		IEditorPart editor = window.getActivePage().getActiveEditor();
//		if(!(editor instanceof ITextEditor)) return null;
//
//		ITextEditor textEditor = (ITextEditor) editor;
//
//		IDocument eclipseDocument = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
//
//		AtomicBoolean ignoreChanges = new AtomicBoolean(false);
//		SharedDocument doc = new SharedDocument(con);
//		doc.localInsert(0, eclipseDocument.get());
//
//		doc.addListener(new DocumentListener() {
//
//			@Override
//			public void onInsert(int index, char character) {
//				Display.getDefault().asyncExec(() -> {
//					try {
//						ignoreChanges.set(true);
//						eclipseDocument.replace(index, 0, String.valueOf(character));
//						ignoreChanges.set(false);
//					} catch (BadLocationException e) {
//						e.printStackTrace();
//					}
//				});
//
//			}
//
//			@Override
//			public void onDelete(int index) {
//				Display.getDefault().asyncExec(() -> {
//					try {
//						ignoreChanges.set(true);
//						eclipseDocument.replace(index, 1, "");
//						ignoreChanges.set(false);
//					} catch (BadLocationException e) {
//						e.printStackTrace();
//					}
//				});
//			}
//		});
//
//		eclipseDocument.addDocumentListener(new IDocumentListener() {
//
//			@Override
//			public void documentChanged(DocumentEvent event) {
//				if(ignoreChanges.get()) return; // TODO: not very ideal
//
//				if(event.getLength() > 0) {
//					doc.localDelete(event.getOffset(), event.getLength());
//				}
//
//				doc.localInsert(event.getOffset(), event.getText());
//			}
//
//			@Override
//			public void documentAboutToBeChanged(DocumentEvent event) {
//
//			}
//		});

		return null;
	}
}
