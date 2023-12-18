package me.mrletsplay.shareclient.util.listeners;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.widgets.Display;

import me.mrletsplay.shareclient.ShareClient;
import me.mrletsplay.shareclient.util.ProjectRelativePath;
import me.mrletsplay.shareclient.util.ShareSession;
import me.mrletsplay.shareclientcore.document.SharedDocument;

public class ShareClientDocumentListener implements IDocumentListener {

	private ProjectRelativePath path;
	private IDocument document;

	private boolean ignoreChanges = false;

	public ShareClientDocumentListener(ProjectRelativePath path, IDocument document) {
		this.path = path;
		this.document = document;
	}

	public void setIgnoreChanges(boolean ignoreChanges) {
		this.ignoreChanges = ignoreChanges;
	}

	public boolean isIgnoreChanges() {
		return ignoreChanges;
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		if(ignoreChanges) return;

		System.out.println("UPDATE ON THREAD " + Thread.currentThread());

		Display.getDefault().asyncExec(() -> {
			ShareSession session = ShareClient.getDefault().getActiveSession();
			if(session == null) return;

			SharedDocument doc = session.getOrCreateSharedDocument(path, () -> event.fDocument.get());
			if(event.getLength() > 0) {
				doc.localDelete(event.getOffset(), event.getLength());
			}

			doc.localInsert(event.getOffset(), event.getText());
		});
	}

	@Override
	public void documentChanged(DocumentEvent event) {

	}

	public IDocument getDocument() {
		return document;
	}

}
