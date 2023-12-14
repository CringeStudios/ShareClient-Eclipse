package me.mrletsplay.shareclient.util.listeners;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import me.mrletsplay.shareclient.util.ProjectRelativePath;

public class ShareClientPartListener implements IPartListener2 {

	private Map<ProjectRelativePath, IDocumentListener> listeners = new HashMap<>();

	private IDocumentListener createListener(ProjectRelativePath path) {
		if(listeners.containsKey(path)) return listeners.get(path);

		IDocumentListener listener = new IDocumentListener() {

			private boolean ignoreChanges = false;

			@Override
			public void documentChanged(DocumentEvent event) {
				System.out.println("Change in document at " + path);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {

			}
		};
		listeners.put(path, listener);
		return listener;
	}

	public void addDocumentListener(IWorkbenchPartReference partRef) {
		IWorkbenchPart part = partRef.getPart(false);
		if(!(part instanceof IEditorPart)) return;
		IEditorPart editor = (IEditorPart) part;
		if(!(editor instanceof ITextEditor)) return;
		ITextEditor textEditor = (ITextEditor) editor;
		IEditorInput editorInput = editor.getEditorInput();
		if(!(editorInput instanceof FileEditorInput)) return;
		FileEditorInput fileEditorInput = (FileEditorInput) editorInput;
		IDocument document = textEditor.getDocumentProvider().getDocument(editorInput);

		IFile file = fileEditorInput.getFile();
		IProject project = file.getProject();

		Path filePath = project.getLocation().toPath().relativize(file.getLocation().toPath());
		ProjectRelativePath relPath = new ProjectRelativePath(project.getName(), filePath.toString());
		System.out.println(relPath);
		document.addDocumentListener(createListener(relPath));
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		addDocumentListener(partRef);
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		addDocumentListener(partRef);
	}

}
