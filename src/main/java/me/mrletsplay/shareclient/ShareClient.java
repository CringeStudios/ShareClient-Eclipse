package me.mrletsplay.shareclient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import me.mrletsplay.shareclient.util.ChecksumUtil;
import me.mrletsplay.shareclient.util.Peer;
import me.mrletsplay.shareclient.util.ProjectRelativePath;
import me.mrletsplay.shareclient.util.ShareSession;
import me.mrletsplay.shareclient.util.listeners.ShareClientPageListener;
import me.mrletsplay.shareclient.util.listeners.ShareClientPartListener;
import me.mrletsplay.shareclient.util.listeners.ShareClientWindowListener;
import me.mrletsplay.shareclient.views.ShareView;
import me.mrletsplay.shareclientcore.connection.Change;
import me.mrletsplay.shareclientcore.connection.ConnectionException;
import me.mrletsplay.shareclientcore.connection.MessageListener;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.WebSocketConnection;
import me.mrletsplay.shareclientcore.connection.message.AddressableMessage;
import me.mrletsplay.shareclientcore.connection.message.ChangeMessage;
import me.mrletsplay.shareclientcore.connection.message.ChecksumMessage;
import me.mrletsplay.shareclientcore.connection.message.FullSyncMessage;
import me.mrletsplay.shareclientcore.connection.message.Message;
import me.mrletsplay.shareclientcore.connection.message.PeerJoinMessage;
import me.mrletsplay.shareclientcore.connection.message.PeerLeaveMessage;
import me.mrletsplay.shareclientcore.connection.message.RequestFullSyncMessage;
import me.mrletsplay.shareclientcore.document.SharedDocument;

/**
 * The activator class controls the plug-in life cycle
 */
public class ShareClient extends AbstractUIPlugin implements MessageListener, IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = "ShareClient"; //$NON-NLS-1$

	// The shared instance
	private static ShareClient plugin;

	private ShareClientPartListener partListener = new ShareClientPartListener();

	private ShareView view;
	private ShareSession activeSession;

	private Map<ProjectRelativePath, SharedDocument> sharedDocuments;

	public ShareClient() {
		this.sharedDocuments = new HashMap<>();
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

		if(message instanceof PeerLeaveMessage leave) {
			activeSession.getPeers().removeIf(p -> p.siteID() == leave.peerSiteID());
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

				// TODO: update sharedDocuments

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
				if(!sendFullSyncOrChecksum(connection, req.siteID(), en.getKey(), en.getValue(), false)) return;
			}
		}

		if(message instanceof ChangeMessage change) {
			Change c = change.change();
			try {
				ProjectRelativePath path = ProjectRelativePath.of(c.documentPath());
			}catch(IllegalArgumentException e) {
				return;
			}

			// TODO: insert change into document in sharedDocuments
		}
	}

	public void addSharedProject(IProject project) {
		activeSession.getSharedProjects().add(project);

		RemoteConnection connection = activeSession.getConnection();
		for(Map.Entry<ProjectRelativePath, Path> en : getProjectFiles(project).entrySet()) {
			// TODO: add new document to sharedDocuments
			if(!sendFullSyncOrChecksum(connection, AddressableMessage.BROADCAST_SITE_ID, en.getKey(), en.getValue(), false)) return;
		}
	}

	private boolean sendFullSyncOrChecksum(RemoteConnection connection, int siteID, ProjectRelativePath relativePath, Path filePath, boolean checksum) {
		if (!Files.isRegularFile(filePath)) return false;

		try {
			byte[] bytes = Files.readAllBytes(filePath);

			if(!checksum) {
				connection.send(new FullSyncMessage(siteID, relativePath.toString(), bytes));
			}else {
				connection.send(new ChecksumMessage(siteID, relativePath.toString(), ChecksumUtil.generateSHA256(bytes)));
			}
		} catch (IOException | ConnectionException e) {
			e.printStackTrace();
			MessageDialog.openError(null, "Share Client", "Failed to send file contents: " + e.toString());
			return false;
		}

		return true;
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

	public ShareClientPartListener getPartListener() {
		return partListener;
	}

	@Override
	public void earlyStartup() {
		PlatformUI.getWorkbench().addWindowListener(ShareClientWindowListener.INSTANCE);
		Arrays.stream(PlatformUI.getWorkbench().getWorkbenchWindows()).forEach(w -> {
			w.addPageListener(ShareClientPageListener.INSTANCE);
			Arrays.stream(w.getPages()).forEach(p -> {
				p.addPartListener(partListener);
				Arrays.stream(p.getEditorReferences()).forEach(e -> partListener.addDocumentListener(e));
			});
		});
	}

}
