package me.mrletsplay.shareclient.views;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import me.mrletsplay.shareclient.Activator;
import me.mrletsplay.shareclientcore.connection.ConnectionException;
import me.mrletsplay.shareclientcore.connection.RemoteConnection;
import me.mrletsplay.shareclientcore.connection.message.PeerJoinMessage;
import me.mrletsplay.shareclientcore.connection.message.RequestFullSyncMessage;

public class ShareView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "me.mrletsplay.shareclient.views.ShareView";

	@Inject IWorkbench workbench;

	private TableViewer viewer;

	private List<String> peerNames = new ArrayList<>();

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
			return workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(new String[] {});
		viewer.setLabelProvider(new ViewLabelProvider());

		// Create the help context id for the viewer's control
		workbench.getHelpSystem().setHelp(viewer.getControl(), "ShareClient.viewer");
		getSite().setSelectionProvider(viewer);

		IActionBars bars = getViewSite().getActionBars();

		Action joinSession = new Action("Join session", ImageDescriptor.createFromFile(ShareView.class, "/icons/door.png")) {

			@Override
			public void run() {
				InputDialog input = new InputDialog(viewer.getControl().getShell(), "Join session", "Enter session id", "EEE", null);
				input.setBlockOnOpen(true);
				if(input.open() != InputDialog.OK) return;

				RemoteConnection connection = Activator.getDefault().getActiveConnection();
				if(connection != null) connection.disconnect();

				connection = Activator.getDefault().openConnection(input.getValue());
				if(connection == null) return;

				connection.addListener(m -> {
					System.out.println("Got: " + m);
					if(m instanceof PeerJoinMessage join) {
						peerNames.add(join.peerName());
						Display.getDefault().asyncExec(() -> viewer.setInput(peerNames.toArray(String[]::new)));
					}

					// TODO: handle FULL_SYNC
				});

				try {
					connection.send(new RequestFullSyncMessage(connection.getSiteID(), null));
				} catch (ConnectionException e) {
					connection.disconnect();
					showMessage("Failed to send: " + e);
				}
			}

		};
		bars.getToolBarManager().add(joinSession);

		Action showSettings = new Action("Settings", ImageDescriptor.createFromFile(ShareView.class, "/icons/cog.png")) {

			@Override
			public void run() {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
					null, "ShareClient.preferences",
					null, null);
				dialog.open();
			}

		};
		bars.getToolBarManager().add(showSettings);

		viewer.addDoubleClickListener(event -> {
			showMessage(Arrays.toString(((StructuredSelection) event.getSelection()).toArray()));
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Share Client",
			message);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
