package me.mrletsplay.shareclient.views;


import java.util.Arrays;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import me.mrletsplay.shareclient.ShareClient;
import me.mrletsplay.shareclient.util.ShareSession;
import me.mrletsplay.shareclientcore.connection.ConnectionException;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.message.RequestFullSyncMessage;

public class ShareView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "me.mrletsplay.shareclient.views.ShareView";

	@Inject IWorkbench workbench;

	private TableViewer viewer;

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		@Override
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		@Override
		public Image getImage(Object obj) {
			return ImageDescriptor.createFromFile(ShareView.class, "/icons/account.png").createImage();
		}
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		System.out.println(ShareClient.getDefault());
		ShareClient.getDefault().setView(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		ShareClient.getDefault().setView(null);
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(new String[0]);
		viewer.setLabelProvider(new ViewLabelProvider());

		// Create the help context id for the viewer's control
		workbench.getHelpSystem().setHelp(viewer.getControl(), "ShareClient.viewer");
		getSite().setSelectionProvider(viewer);

		viewer.addDoubleClickListener(event -> {
			showMessage(Arrays.toString(((StructuredSelection) event.getSelection()).toArray()));
		});

		updateActionBars();
	}

	public void updateActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager toolbars = bars.getToolBarManager();

		toolbars.removeAll();

		Action joinSession = new Action("Join session", ImageDescriptor.createFromFile(ShareView.class, "/icons/door.png")) {

			@Override
			public void run() {
				InputDialog input = new InputDialog(viewer.getControl().getShell(), "Join session", "Enter session id", "EEE", null);
				input.setBlockOnOpen(true);
				if(input.open() != InputDialog.OK) return;

				ShareSession session = ShareClient.getDefault().getActiveSession();
				if(session != null) session.stop();

				session = ShareClient.getDefault().startSession(input.getValue());
				if(session == null) return;

				RemoteConnection connection = session.getConnection();

				try {
					connection.send(new RequestFullSyncMessage(connection.getSiteID(), null));
					updateActionBars();
				} catch (ConnectionException e) {
					ShareClient.getDefault().closeConnection();
					showMessage("Failed to send: " + e);
				}
			}

		};

		Action copySessionID = new Action("Copy session ID", ImageDescriptor.createFromFile(ShareView.class, "/icons/content-copy.png")) {

			@Override
			public void run() {
				Clipboard clipboard = new Clipboard(Display.getDefault());
				clipboard.setContents(new String[] {ShareClient.getDefault().getActiveSession().getSessionID()}, new Transfer[] {TextTransfer.getInstance()});
			}

		};


		Action leaveSession = new Action("Leave session", ImageDescriptor.createFromFile(ShareView.class, "/icons/stop.png")) {

			@Override
			public void run() {
				ShareClient.getDefault().closeConnection();
				updateActionBars();
			}

		};

		Action showSettings = new Action("Settings", ImageDescriptor.createFromFile(ShareView.class, "/icons/cog.png")) {

			@Override
			public void run() {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
					null, "ShareClient.preferences",
					null, null);
				dialog.open();
			}

		};

		if(ShareClient.getDefault().getActiveSession() == null) {
			toolbars.add(joinSession);
		}else {
			toolbars.add(copySessionID);
			toolbars.add(leaveSession);
		}

		toolbars.add(showSettings);

		bars.updateActionBars();
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Share Client",
			message);
	}

	public TableViewer getViewer() {
		return viewer;
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
