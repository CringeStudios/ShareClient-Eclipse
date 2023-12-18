package me.mrletsplay.shareclient.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;

import me.mrletsplay.shareclient.ShareClient;
import me.mrletsplay.shareclient.util.listeners.ShareClientDocumentListener;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.document.DocumentListener;
import me.mrletsplay.shareclientcore.document.SharedDocument;

public class ShareSession {

	private RemoteConnection connection;
	private String sessionID;
	private List<IProject> sharedProjects;
	private Map<ProjectRelativePath, SharedDocument> sharedDocuments;
	private List<Peer> peers;

	public ShareSession(RemoteConnection connection, String sessionID) {
		this.connection = connection;
		this.sessionID = sessionID;
		this.sharedProjects = new ArrayList<>();
		this.sharedDocuments = new HashMap<>();
		this.peers = new ArrayList<>();
	}

	public RemoteConnection getConnection() {
		return connection;
	}

	public String getSessionID() {
		return sessionID;
	}

	public List<IProject> getSharedProjects() {
		return sharedProjects;
	}

	public Map<ProjectRelativePath, SharedDocument> getSharedDocuments() {
		return sharedDocuments;
	}


	public SharedDocument getSharedDocument(ProjectRelativePath path) {
		return sharedDocuments.get(path);
	}

	public SharedDocument getOrCreateSharedDocument(ProjectRelativePath path, Supplier<String> initialContents) {
		return sharedDocuments.computeIfAbsent(path, p -> {
			SharedDocument doc = new SharedDocument(connection, path.toString(), initialContents.get());

			doc.addListener(new DocumentListener() {

				@Override
				public void onInsert(int index, char character) {
					Display.getDefault().asyncExec(() -> {
						ShareClientDocumentListener documentListener = ShareClient.getDefault().getPartListener().getListeners().get(path);
						if(documentListener != null) {
							IDocument document = documentListener.getDocument();

							documentListener.setIgnoreChanges(true);
							try {
								document.replace(index, 0, String.valueOf(character));
							} catch (BadLocationException e) {
								e.printStackTrace();
								// TODO: treat as inconsistency
//								MessageDialog.openError(null, "Share Client", "Failed to update document: " + e.toString());
							}
							documentListener.setIgnoreChanges(false);
						}
					});
				}

				@Override
				public void onDelete(int index) {
					Display.getDefault().asyncExec(() -> {
						ShareClientDocumentListener documentListener = ShareClient.getDefault().getPartListener().getListeners().get(path);
						if(documentListener != null) {
							IDocument document = documentListener.getDocument();

							documentListener.setIgnoreChanges(true);
							try {
								document.replace(index, 1, "");
							} catch (BadLocationException e) {
								e.printStackTrace();
								// TODO: treat as inconsistency
//								MessageDialog.openError(null, "Share Client", "Failed to update document: " + e.toString());
							}
							documentListener.setIgnoreChanges(false);
						}
					});
				}
			});

			return doc;
		});
	}

	public List<Peer> getPeers() {
		return peers;
	}

	public boolean hasRemotePeer() {
		return peers.stream().anyMatch(p -> p.siteID() != connection.getSiteID());
	}

	public boolean isHost() {
		return peers.stream().max(Comparator.comparingInt(p -> p.siteID())).get().siteID() == connection.getSiteID();
	}

	public void stop() {
		connection.disconnect();
		ShareClient.getDefault().updateView();
	}

}
