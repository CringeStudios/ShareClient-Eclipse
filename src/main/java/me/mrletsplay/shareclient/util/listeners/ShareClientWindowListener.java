package me.mrletsplay.shareclient.util.listeners;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;

public class ShareClientWindowListener implements IWindowListener {

	public static final ShareClientWindowListener INSTANCE = new ShareClientWindowListener();

	private ShareClientWindowListener() {}

	@Override
	public void windowOpened(IWorkbenchWindow window) {
		window.addPageListener(ShareClientPageListener.INSTANCE);
	}

	@Override
	public void windowClosed(IWorkbenchWindow window) {
		window.removePageListener(ShareClientPageListener.INSTANCE);
	}

	@Override
	public void windowActivated(IWorkbenchWindow window) {}

	@Override
	public void windowDeactivated(IWorkbenchWindow window) {}

}
