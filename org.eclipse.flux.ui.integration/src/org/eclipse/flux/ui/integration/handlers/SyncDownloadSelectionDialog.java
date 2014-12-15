/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.ui.integration.handlers;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.eclipse.flux.client.CallbackIDAwareMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class SyncDownloadSelectionDialog extends ElementListSelectionDialog {

	private final MessageConnector messagingConnector;
	private final String username;
	private CallbackIDAwareMessageHandler responseHandler;
	private Set<String> projects;

	public SyncDownloadSelectionDialog(final Shell parent, final ILabelProvider renderer, final MessageConnector messagingConnector, final String username) {
		super(parent, renderer);
		this.messagingConnector = messagingConnector;
		this.username = username;

		this.setMultipleSelection(true);
		this.setAllowDuplicates(false);
		this.setTitle("Import Synced Projects...");
		
		this.projects = new ConcurrentSkipListSet<>();
	}
	
	@Override
	public int open() {
		try {
			int callbackID = this.hashCode();
			
			projects.clear();
			
			responseHandler = new CallbackIDAwareMessageHandler("getProjectsResponse", callbackID) {
				@Override
				public void handle(String messageType, JSONObject response) {
					try {
						boolean newProjects = false;

						JSONArray projectsList = response.getJSONArray("projects");
						for (int i = 0; i < projectsList.length(); i++) {
							JSONObject project = projectsList.getJSONObject(i);
							String projectName = project.getString("name");

							newProjects |= projects.add(projectName);
						}

						if (newProjects) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									setListElements((String[]) projects.toArray(new String[projects.size()]));
								}
							});
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			
			this.messagingConnector.addMessageHandler(responseHandler);
			
			JSONObject message = new JSONObject();
			message.put("callback_id", callbackID);
			message.put("username", username);
			this.messagingConnector.send("getProjectsRequest", message);
		} catch (Exception e1) {
			e1.printStackTrace();
		}		
		
		return super.open();
	}
	
	@Override
	public boolean close() {
		messagingConnector.removeMessageHandler(responseHandler);
		return super.close();
	}

}
