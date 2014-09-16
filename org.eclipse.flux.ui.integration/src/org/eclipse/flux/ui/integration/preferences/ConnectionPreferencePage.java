/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.ui.integration.preferences;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.flux.core.Activator;
import org.eclipse.flux.core.IPreferenceConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Flux connection preferences page
 * 
 * @author aboyko
 *
 */
public class ConnectionPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	final private boolean editable; 
	private Text url;
	private Text user;
	private Text token;
	
	public ConnectionPreferencePage() {
		super();
		this.editable = Activator.isConnectionSettingsViaPreferences();
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite mainComposite = new Composite(parent, parent.getStyle());
		mainComposite.setLayout(new GridLayout());
		createConnectionGroup(mainComposite).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));;
		return mainComposite;
	}
	
	private Control createConnectionGroup(Composite parent) {
		Group group = new Group(parent, SWT.BORDER_SOLID);
		group.setText("Connection Settings");
		group.setToolTipText("Flux messaging server connection info");
		group.setLayout(new GridLayout(2, false));
		
		Label label = new Label(group, SWT.NONE);
		label.setText("Server URL:");
		GridData gridData = new GridData();
		label.setLayoutData(gridData);
		
		url = new Text(group, SWT.BORDER);
		url.setText(editable ? getPreferenceStore().getString(IPreferenceConstants.PREF_URL) : Activator.getHostUrl());
		url.setEnabled(editable);
		url.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				if (url.getText().isEmpty()) {
					setErrorMessage(null);
					setValid(true);
				} else {
					try {
						new URL(url.getText());
						setErrorMessage(null);
						setValid(true);
					} catch (MalformedURLException e) {
						setErrorMessage(e.getMessage());
						setValid(false);
					}
				}
			}		
		});
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		url.setLayoutData(gridData);
		
		label = new Label(group, SWT.NONE);
		label.setText("GitHub ID:");
		label.setLayoutData(new GridData());
		
		user = new Text(group, SWT.BORDER);
		user.setText(editable ? getPreferenceStore().getString(IPreferenceConstants.PREF_USER_ID) : Activator.getUserId());
		user.setEnabled(editable);
		user.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = new Label(group, SWT.NONE);
		label.setText("GitHub Token:");
		label.setLayoutData(new GridData());
		
		token = new Text(group, SWT.BORDER);
		token.setText(editable ? getPreferenceStore().getString(IPreferenceConstants.PREF_USER_TOKEN) : Activator.getUserToken());
		token.setEnabled(editable);
		token.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		return group;
	}

	@Override
	protected void performDefaults() {
		if (editable) {
			url.setText(getPreferenceStore().getDefaultString(IPreferenceConstants.PREF_URL));
			user.setText(getPreferenceStore().getDefaultString(IPreferenceConstants.PREF_USER_ID));
			token.setText(getPreferenceStore().getDefaultString(IPreferenceConstants.PREF_USER_TOKEN));
		}
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		if (editable && (!getPreferenceStore().getString(IPreferenceConstants.PREF_URL).equals(url.getText())
				|| !getPreferenceStore().getString(IPreferenceConstants.PREF_USER_ID).equals(user.getText())
				|| !getPreferenceStore().getString(IPreferenceConstants.PREF_USER_TOKEN).equals(token.getText()))) {
			if (MessageDialog.openQuestion(getShell(), "Change Flux Server Connection Settings", "Do you want to change Flux server connection settings?\n\nIf \"Yes\" is selected workbench will be restarted and new settings will take effect.\n\nEnsure all work is saved before clicking \"Yes\"")) {
				getPreferenceStore().putValue(IPreferenceConstants.PREF_URL, url.getText());
				getPreferenceStore().putValue(IPreferenceConstants.PREF_USER_ID, user.getText());
				getPreferenceStore().putValue(IPreferenceConstants.PREF_USER_TOKEN, token.getText());
				return PlatformUI.getWorkbench().restart();
			} else {
				return false;
			}
		}
		return super.performOk();
	}

	@Override
	public void init(IWorkbench workbench) {
		setDescription("Allows setting of various Flux server connection parameters.");
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID));
		if (!editable) {
			setMessage("Settings cannot be changed from UI. They are set via environment variable and/or command line arguments", IMessageProvider.WARNING);
		}
	}

}
