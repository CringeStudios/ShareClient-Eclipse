package me.mrletsplay.shareclient;

import java.net.URI;
import java.util.Random;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import me.mrletsplay.shareclientcore.connection.ConnectionException;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.WebSocketConnection;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "ShareClient"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private RemoteConnection activeConnection;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		System.out.println("STARTING");
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
	public static Activator getDefault() {
		return plugin;
	}

	public RemoteConnection openConnection(String sessionID) {
		String serverURI = getPreferenceStore().getString(ShareClientPreferences.SERVER_URI);
		if(serverURI == null) return null;

		String username = getPreferenceStore().getString(ShareClientPreferences.USERNAME);
		if(username == null || username.isBlank()) username = "user" + new Random().nextInt(1000);

		activeConnection = new WebSocketConnection(URI.create(serverURI), username);
		try {
			activeConnection.connect(sessionID); // TODO: connect to existing session
		} catch (ConnectionException e) {
			MessageDialog.openInformation(
				null,
				"Share Client",
				"Failed to connect to server: " + e);
			activeConnection = null;
			return null;
		}

		return activeConnection;
	}

	public RemoteConnection getOrOpenConnection() {
		if(activeConnection == null) {
			openConnection(UUID.randomUUID().toString());
		}

		return activeConnection;
	}

	public RemoteConnection getActiveConnection() {
		return activeConnection;
	}

}
