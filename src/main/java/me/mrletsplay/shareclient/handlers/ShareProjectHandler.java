package me.mrletsplay.shareclient.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import me.mrletsplay.shareclient.ShareClient;
import me.mrletsplay.shareclient.util.ShareSession;

public class ShareProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
		if(selection.isEmpty()) return null;

		IResource res = Adapters.adapt(selection.getFirstElement(), IResource.class, false);
		if(res == null) return null;

		IProject project = res.getProject();
		if(project == null) return null;

		IPath path = project.getLocation();
		if(path == null) return null;

		ShareSession session = ShareClient.getDefault().getOrStartSession();
		if(session == null) return null;

		ShareClient.getDefault().addSharedProject(project);
		return null;
	}
}
