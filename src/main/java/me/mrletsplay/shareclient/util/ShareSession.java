package me.mrletsplay.shareclient.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IProject;

import me.mrletsplay.shareclient.ShareClient;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;

public class ShareSession {

	private RemoteConnection connection;
	private String sessionID;
	private List<IProject> sharedProjects;
	private List<Peer> peers;

	public ShareSession(RemoteConnection connection, String sessionID) {
		this.connection = connection;
		this.sessionID = sessionID;
		this.sharedProjects = new ArrayList<>();
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
