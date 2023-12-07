package me.mrletsplay.shareclient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import me.mrletsplay.shareclient.util.Peer;
import me.mrletsplay.shareclient.util.ProjectRelativePath;
import me.mrletsplay.shareclient.views.ShareView;
import me.mrletsplay.shareclientcore.connection.ConnectionException;
import me.mrletsplay.shareclientcore.connection.MessageListener;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.WebSocketConnection;
import me.mrletsplay.shareclientcore.connection.message.FullSyncMessage;
import me.mrletsplay.shareclientcore.connection.message.Message;
import me.mrletsplay.shareclientcore.connection.message.PeerJoinMessage;

/**
 * The activator class controls the plug-in life cycle
 */
public class ShareClient extends AbstractUIPlugin implements MessageListener {

	// The plug-in ID
	public static final String PLUGIN_ID = "ShareClient"; //$NON-NLS-1$

	// The shared instance
	private static ShareClient plugin;

	private RemoteConnection activeConnection;
	private ShareView view;
	private List<Peer> peers;

	/**
	 * The constructor
	 */
	public ShareClient() {
		this.peers = new ArrayList<>();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		getPreferenceStore().setDefault(ShareClientPreferences.SERVER_URI, "ws://localhost:5473");
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

	private void updateView() {
		if(view == null) return;
		Display.getDefault().asyncExec(() -> view.getViewer().setInput(peers.stream().map(p -> p.name()).toArray(String[]::new)));
	}

	public RemoteConnection openConnection(String sessionID) {
		String serverURI = getPreferenceStore().getString(ShareClientPreferences.SERVER_URI);
		if(serverURI == null) return null;

		String username = getPreferenceStore().getString(ShareClientPreferences.USERNAME);
		if(username == null || username.isBlank()) username = "user" + new Random().nextInt(1000);

		WebSocketConnection connection = new WebSocketConnection(URI.create(serverURI), username);
		try {
			connection.connect(sessionID); // TODO: connect to existing session
		} catch (ConnectionException e) {
			MessageDialog.openInformation(
				null,
				"Share Client",
				"Failed to connect to server: " + e);
			return null;
		}

		connection.addListener(this);

		return activeConnection = connection;
	}

	public RemoteConnection getOrOpenConnection() {
		if(activeConnection == null) {
			openConnection(UUID.randomUUID().toString());
		}

		return activeConnection;
	}

	public void closeConnection() {
		if(activeConnection == null) return;
		activeConnection.disconnect();
		activeConnection = null;
		peers.clear();
		updateView();
	}

	public RemoteConnection getActiveConnection() {
		return activeConnection;
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
		if(message instanceof PeerJoinMessage join) {
			peers.add(new Peer(join.peerName(), join.peerSiteID()));
			updateView();
		}

		if(message instanceof FullSyncMessage sync) {
			// TODO: handle FULL_SYNC
			ProjectRelativePath path;
			try {
				path = ProjectRelativePath.of(sync.documentPath());
			}catch(IllegalArgumentException e) {
				return;
			}

			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.projectName());
			if(project == null) return; // TODO: create project

			Path filePath = project.getLocation().toPath().resolve(path.relativePath());
			try {
				if(!Files.exists(filePath)) {
					Files.createDirectories(filePath.getParent());
					Files.createFile(filePath);
				}

				Files.write(filePath, sync.content());
			} catch (IOException e) {
				// TODO: handle exception
			}
		}
	}

}
