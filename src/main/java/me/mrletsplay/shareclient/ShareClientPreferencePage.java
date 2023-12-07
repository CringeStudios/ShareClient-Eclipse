package me.mrletsplay.shareclient;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class ShareClientPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ShareClientPreferencePage() {
//		super(GRID);
		setPreferenceStore(ShareClient.getDefault().getPreferenceStore());
		setDescription("Share Client Preferences");
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(ShareClientPreferences.SERVER_URI, "Server URI", getFieldEditorParent()));
		addField(new StringFieldEditor(ShareClientPreferences.USERNAME, "Display Name", getFieldEditorParent()));
		addField(new BooleanFieldEditor(ShareClientPreferences.SHOW_CURSORS, "Show other users' cursors", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {

	}

}
