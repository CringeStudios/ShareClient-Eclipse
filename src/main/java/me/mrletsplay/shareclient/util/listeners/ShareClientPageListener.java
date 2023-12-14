package me.mrletsplay.shareclient.util.listeners;

import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IWorkbenchPage;

import me.mrletsplay.shareclient.ShareClient;

public class ShareClientPageListener implements IPageListener {

	public static final ShareClientPageListener INSTANCE = new ShareClientPageListener();

	private ShareClientPageListener() {}

	@Override
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(ShareClient.getDefault().getPartListener());
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
		page.removePartListener(ShareClient.getDefault().getPartListener());
	}

	@Override
	public void pageActivated(IWorkbenchPage page) {}

}
