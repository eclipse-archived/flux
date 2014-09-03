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
package org.eclipse.flux.cloudfoundry.deployment.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.flux.client.CallbackIDAwareMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 * @author Kris De Volder (copied from org.eclipse.flux.core and 'purified' (i.e. remove eclipse api references and use plain 
 * 		java.io.File stuffs instead.
 */
public class DownloadProject {

	public interface CompletionCallback {
		public void downloadComplete(File project);

		public void downloadFailed();
	}

	private MessageConnector messagingConnector;

	private String projectName;
	private int callbackID;
	private CompletionCallback completionCallback;

	private String username;
	private File project;

	private AtomicInteger requestedFileCount = new AtomicInteger(0);
	private AtomicInteger downloadedFileCount = new AtomicInteger(0);

	private CallbackIDAwareMessageHandler projectResponseHandler;
	private CallbackIDAwareMessageHandler resourceResponseHandler;

	public DownloadProject(MessageConnector flux, String projectName, String username) {
		this.messagingConnector = flux;
		this.projectName = projectName;
		this.username = username;

		this.callbackID = this.hashCode();

		projectResponseHandler = new CallbackIDAwareMessageHandler("getProjectResponse", this.callbackID) {
			@Override
			public void handle(String messageType, JSONObject message) {
				getProjectResponse(message);
			}
		};
		resourceResponseHandler = new CallbackIDAwareMessageHandler("getResourceResponse", this.callbackID) {
			@Override
			public void handle(String messageType, JSONObject message) {
				getResourceResponse(message);
			}
		};
	}

	public void run(CompletionCallback completionCallback) {
		this.messagingConnector.addMessageHandler(projectResponseHandler);
		this.messagingConnector.addMessageHandler(resourceResponseHandler);

		this.completionCallback = completionCallback;

		try {
			project = createTmpDir(username+"."+projectName);

			JSONObject message = new JSONObject();
			message.put("callback_id", this.callbackID);
			message.put("username", this.username);
			message.put("project", this.projectName);

			messagingConnector.send("getProjectRequest", message);
		} catch (Exception e1) {
			e1.printStackTrace();
			this.messagingConnector.removeMessageHandler(projectResponseHandler);
			this.messagingConnector.removeMessageHandler(resourceResponseHandler);
			this.completionCallback.downloadFailed();
		}
	}

	private static File createTmpDir(String prefix) throws IOException {
		final File temp;
		temp = File.createTempFile(prefix, null);
		if(!(temp.delete()))
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!(temp.mkdir()))
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return (temp);
	}
	
	public void getProjectResponse(JSONObject response) {
		try {
			final String responseProject = response.getString("project");
			final String responseUser = response.getString("username");
			final JSONArray files = response.getJSONArray("files");

			if (this.username.equals(responseUser)) {
				for (int i = 0; i < files.length(); i++) {
					JSONObject resource = files.getJSONObject(i);

					String resourcePath = resource.getString("path");
					long timestamp = resource.getLong("timestamp");

					String type = resource.optString("type");

					if (type.equals("folder")) {
						if (resourcePath.isEmpty()) {
							project.setLastModified(timestamp);
						} else {
							File folder = new File(project, resourcePath);
							if (!folder.exists()) {
								folder.mkdirs();
							}
							folder.setLastModified(timestamp);
						}
					} else if (type.equals("file")) {
						requestedFileCount.incrementAndGet();
					}
				}

				for (int i = 0; i < files.length(); i++) {
					JSONObject resource = files.getJSONObject(i);

					String resourcePath = resource.getString("path");
					String type = resource.optString("type");

					if (type.equals("file")) {
						JSONObject message = new JSONObject();
						message.put("callback_id", callbackID);
						message.put("username", this.username);
						message.put("project", responseProject);
						message.put("resource", resourcePath);

						messagingConnector.send("getResourceRequest", message);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.messagingConnector.removeMessageHandler(projectResponseHandler);
			this.messagingConnector.removeMessageHandler(resourceResponseHandler);
			this.completionCallback.downloadFailed();
		}
	}

	public void getResourceResponse(JSONObject response) {
		try {
			final String responseUser = response.getString("username");
			final String resourcePath = response.getString("resource");
			final long timestamp = response.getLong("timestamp");
			final String content = response.getString("content");

			if (this.username.equals(responseUser)) {
				File file = new File(project, resourcePath);
				IOUtil.pipe(new ByteArrayInputStream(content.getBytes()), file);
				file.setLastModified(timestamp);

				int downloaded = this.downloadedFileCount.incrementAndGet();
				if (downloaded == this.requestedFileCount.get()) {
					this.messagingConnector.removeMessageHandler(projectResponseHandler);
					this.messagingConnector.removeMessageHandler(resourceResponseHandler);
					this.completionCallback.downloadComplete(project);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.messagingConnector.removeMessageHandler(projectResponseHandler);
			this.messagingConnector.removeMessageHandler(resourceResponseHandler);
			this.completionCallback.downloadFailed();
		}
	}

// doens't quite work, creates a deadlock of sorts when called in a message handler. This because that stops sock.io from receiving
//  responses. Should be able to solve this by executing something on a different thread.
//	public static File download(MessageConnector flux, final String projectName, final String username) throws Exception {
//		DownloadProject download = new DownloadProject(flux, projectName, username);
//		final BasicFuture<File> future = new BasicFuture<File>();
//		download.run(new CompletionCallback() {
//			@Override
//			public void downloadFailed() {
//				future.reject(new IOException("Downloading of project '"+username+"/"+projectName+"' failed"));
//			}
//			
//			@Override
//			public void downloadComplete(File project) {
//				future.resolve(project);
//			}
//		});
//		return future.get();
//	}

}
