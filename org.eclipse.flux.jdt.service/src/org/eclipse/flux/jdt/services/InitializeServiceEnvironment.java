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
package org.eclipse.flux.jdt.services;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.flux.core.AbstractMessageHandler;
import org.eclipse.flux.core.CallbackIDAwareMessageHandler;
import org.eclipse.flux.core.DownloadProject;
import org.eclipse.flux.core.DownloadProject.CompletionCallback;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.Repository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * For each project that is found or that becomes connected, initialize it for this workspace
 * (create, open, and build as necessary).
 * @author Martin Lippert
 */
public class InitializeServiceEnvironment {

	private static int GET_PROJECTS_CALLBACK = "InitializeServiceEnvironment - getProjectsCallback".hashCode();

	private final IMessagingConnector messagingConnector;
	final Repository repository;

	private IMessageHandler getProjectsResponseHandler;
	private IMessageHandler projectConnectedHandler;

	public InitializeServiceEnvironment(IMessagingConnector messagingConnector, Repository repository) {
		this.messagingConnector = messagingConnector;
		this.repository = repository;
	}

	public void start() {
		getProjectsResponseHandler = new CallbackIDAwareMessageHandler("getProjectsResponse", GET_PROJECTS_CALLBACK) {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleGetProjectsResponse(message);
			}
		};
		messagingConnector.addMessageHandler(getProjectsResponseHandler);

		projectConnectedHandler = new AbstractMessageHandler("projectConnected") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleProjectConnected(message);
			}
		};
		messagingConnector.addMessageHandler(projectConnectedHandler);

		try {
			JSONObject message = new JSONObject();
			message.put("username", repository.getUsername());
			message.put("callback_id", GET_PROJECTS_CALLBACK);
			this.messagingConnector.send("getProjectsRequest", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void handleGetProjectsResponse(JSONObject message) {
		try {
			String username = message.getString("username");
			if (repository.getUsername().equals(username)) {
				JSONArray projects = message.getJSONArray("projects");
				for (int i = 0; i < projects.length(); i++) {
					JSONObject project = projects.getJSONObject(i);
					String projectName = project.getString("name");
					initializeProject(projectName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void handleProjectConnected(JSONObject message) {
		try {
			String username = message.getString("username");
			String projectName = message.getString("project");

			if (repository.getUsername().equals(username)) {
				initializeProject(projectName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeProject(String projectName) {
		try {
			// already connected project
			if (repository.isConnected(projectName))
				return;

			// project doesn't exist in workspace
			DownloadProject downloadProject = new DownloadProject(messagingConnector, projectName, repository.getUsername());
			downloadProject.run(new CompletionCallback() {
				@Override
				public void downloadFailed() {
				}

				@Override
				public void downloadComplete(IProject downloadedProject) {
					try {
						downloadedProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
					} catch (CoreException e) {
						e.printStackTrace();
					}
					repository.addProject(downloadedProject);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void dispose() {
		messagingConnector.removeMessageHandler(getProjectsResponseHandler);
		messagingConnector.removeMessageHandler(projectConnectedHandler);
	}

}
