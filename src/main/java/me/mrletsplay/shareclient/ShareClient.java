package me.mrletsplay.shareclient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import me.mrletsplay.shareclient.util.Peer;
import me.mrletsplay.shareclient.util.ProjectRelativePath;
import me.mrletsplay.shareclient.util.ShareSession;
import me.mrletsplay.shareclient.views.ShareView;
import me.mrletsplay.shareclientcore.connection.ConnectionException;
import me.mrletsplay.shareclientcore.connection.MessageListener;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.WebSocketConnection;
import me.mrletsplay.shareclientcore.connection.message.FullSyncMessage;
import me.mrletsplay.shareclientcore.connection.message.Message;
import me.mrletsplay.shareclientcore.connection.message.PeerJoinMessage;
import me.mrletsplay.shareclientcore.connection.message.RequestFullSyncMessage;

/**
 * The activator class controls the plug-in life cycle
 */
public class ShareClient extends AbstractUIPlugin implements MessageListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "ShareClient"; //$NON-NLS-1$

	// The shared instance
	private static ShareClient plugin;

	private ShareView view;
	private ShareSession activeSession;

	public ShareClient() {

	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		getPreferenceStore().setDefault(ShareClientPreferences.SERVER_URI, "ws://localhost:5473");
		getPreferenceStore().setDefault(ShareClientPreferences.SHOW_CURSORS, true);
//		new ShareWSClient(URI.create("ws://localhost:5473")).connect();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ShareClient getDefault() {
		return plugin;
	}

	public void updateView() {
		if (view == null) return;
		Display.getDefault().asyncExec(() -> view.updateActionBars());

		if (activeSession == null) return;
		String[] peerNames = activeSession.getPeers().stream().map(p -> p.name()).toArray(String[]::new);
		Display.getDefault().asyncExec(() -> view.getViewer().setInput(peerNames));
	}

	public ShareSession startSession(String sessionID) {
		String serverURI = getPreferenceStore().getString(ShareClientPreferences.SERVER_URI);
		if (serverURI == null) return null;

		String username = getPreferenceStore().getString(ShareClientPreferences.USERNAME);
		if (username == null || username.isBlank())
			username = "user" + new Random().nextInt(1000);

		WebSocketConnection connection = new WebSocketConnection(URI.create(serverURI), username);
		try {
			connection.connect(sessionID); // TODO: connect to existing session
		} catch (ConnectionException e) {
			e.printStackTrace();
			MessageDialog.openError(null, "Share Client", "Failed to connect to server: " + e);
			return null;
		}

		connection.addListener(this);

		updateView();
		return activeSession = new ShareSession(connection, sessionID);
	}

	public ShareSession getOrStartSession() {
		if (activeSession == null) {
			startSession(UUID.randomUUID().toString());
		}

		return activeSession;
	}

	public void closeConnection() {
		if (activeSession == null) return;
		ShareSession session = activeSession;
		activeSession = null;
		session.stop();
		updateView();
	}

	public ShareSession getActiveSession() {
		return activeSession;
	}

	public void setView(ShareView view) {
		this.view = view;
	}

	public ShareView getView() {
		return view;
	}

	@Override
	public void onMessage(Message message) {
		System.out.println("Got: " + message);
		if (message instanceof PeerJoinMessage join) {
			activeSession.getPeers().add(new Peer(join.peerName(), join.peerSiteID()));
			updateView();
		}

		if (message instanceof FullSyncMessage sync) {
			// TODO: handle FULL_SYNC
			ProjectRelativePath path;
			try {
				path = ProjectRelativePath.of(sync.documentPath());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot workspaceRoot = workspace.getRoot();
			IProject project = workspaceRoot.getProject(path.projectName());
			if(project == null) return;
			// TODO: make sure to not overwrite existing non-shared projects
			if (!project.exists()) {
				IProjectDescription description = workspace.newProjectDescription(path.projectName());
				try {
					project.create(description, null);
					project.open(null);
				} catch (CoreException e) {
					e.printStackTrace();
					MessageDialog.openError(null, "Share Client", "Failed to create project: " + e.toString());
					return;
				}
				System.out.println("Created project " + project);
			}

			Path filePath = project.getLocation().toPath().resolve(path.relativePath());
			try {
				if (!Files.exists(filePath)) {
					Files.createDirectories(filePath.getParent());
					Files.createFile(filePath);
				}

				Files.write(filePath, sync.content());
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (IOException | CoreException e) {
				e.printStackTrace();
				MessageDialog.openError(null, "Share Client", "Failed to update file: " + e.toString());
				return;
			}
		}

		if (message instanceof RequestFullSyncMessage req) {
			Map<ProjectRelativePath, Path> paths = new HashMap<>();
			if (req.documentPath() == null) {
				// Sync entire (shared) workspace
				for (IProject project : activeSession.getSharedProjects()) {
					var files = getProjectFiles(project);
					if (files == null) return;
					paths.putAll(files);
				}
			} else {
				ProjectRelativePath path;
				try {
					path = ProjectRelativePath.of(req.documentPath());
				} catch (IllegalArgumentException e) {
					return;
				}

				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.projectName());
				if (project == null || !project.exists()) return;
				if (!activeSession.getSharedProjects().contains(project)) return;

				if (!path.relativePath().isEmpty()) {
					Path projectLocation = project.getLocation().toPath();
					Path filePath = projectLocation.resolve(path.relativePath()).normalize();
					if (!filePath.startsWith(projectLocation)) return;
					paths.put(new ProjectRelativePath(project.getName(), path.relativePath()), filePath);
				} else {
					// Sync entire project
					var files = getProjectFiles(project);
					if (files == null) return;
					paths.putAll(files);
				}
			}

			RemoteConnection connection = activeSession.getConnection();
			for (var en : paths.entrySet()) {
				if (!Files.isRegularFile(en.getValue()))
					continue;
				try {
					byte[] bytes = Files.readAllBytes(en.getValue());
					connection.send(new FullSyncMessage(req.siteID(), en.getKey().toString(), bytes));
				} catch (IOException | ConnectionException e) {
					e.printStackTrace();
					MessageDialog.openError(null, "Share Client", "Failed to send file contents: " + e.toString());
					return;
				}
			}
		}
	}

	private Map<ProjectRelativePath, Path> getProjectFiles(IProject project) {
		try {
			Path projectLocation = project.getLocation().toPath();
			return Files.walk(projectLocation)
					.collect(Collectors.toMap(
							p -> new ProjectRelativePath(project.getName(), projectLocation.relativize(p).toString()),
							Function.identity()));
		} catch (IOException e) {
			e.printStackTrace();
			MessageDialog.openError(null, "Share Client", "Failed to collect project files: " + e.toString());
			return null;
		}
	}

}
