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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.flux.core.CallbackIDAwareMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class SyncDownloadSelectionDialog extends ElementListSelectionDialog {

	private final IMessagingConnector messagingConnector;
	private final String username;

	public SyncDownloadSelectionDialog(final Shell parent, final ILabelProvider renderer, final IMessagingConnector messagingConnector, final String username) {
		super(parent, renderer);
		this.messagingConnector = messagingConnector;
		this.username = username;

		this.setMultipleSelection(true);
		this.setAllowDuplicates(false);
		this.setTitle("Import Synced Projects...");
	}
	
	@Override
	public int open() {
		try {
			int callbackID = this.hashCode();
			
			CallbackIDAwareMessageHandler responseHandler = new CallbackIDAwareMessageHandler("getProjectsResponse", callbackID) {
				@Override
				public void handleMessage(String messageType, JSONObject response) {
					try {
						List<String> projectsNames = new ArrayList<String>();
						JSONArray projects = response.getJSONArray("projects");
						for (int i = 0; i < projects.length(); i++) {
							JSONObject project = projects.getJSONObject(i);
							String projectName = project.getString("name");

							projectsNames.add(projectName);
						}
						setElements((String[]) projectsNames.toArray(new String[projectsNames.size()]));
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					
					messagingConnector.removeMessageHandler(this);
				}
			};
			
			this.messagingConnector.addMessageHandler(responseHandler);
			
			JSONObject message = new JSONObject();
			message.put("callback_id", callbackID);
			message.put("username", username);
			this.messagingConnector.send("getProjectsRequest", message);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}		
		
		return super.open();
	}

}
