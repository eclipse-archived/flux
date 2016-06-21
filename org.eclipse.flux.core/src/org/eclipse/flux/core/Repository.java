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
package org.eclipse.flux.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageBus;
import org.eclipse.flux.watcher.core.RepositoryModule;
import org.eclipse.flux.watcher.core.spi.Project;
import org.eclipse.flux.watcher.fs.JDKProjectModule;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Martin Lippert
 */
public class Repository {

	private String username;
	private MessageConnector messagingConnector;
	private Collection<IMessageHandler> messageHandlers;

	private ConcurrentMap<String, ConnectedProject> syncedProjects;
	private Collection<IRepositoryListener> repositoryListeners;
	
	private AtomicBoolean connected;
	
	private org.eclipse.flux.watcher.core.Repository repository;
	private FluxMessageBus messageBus;

	public Repository(MessageConnector messagingConnector, String user) {
		Injector injector = Guice.createInjector(new RepositoryModule(), new JDKProjectModule());
		this.username = user;
		this.repository = injector.getInstance(org.eclipse.flux.watcher.core.Repository.class);
		this.messageBus = injector.getInstance(FluxMessageBus.class);
		this.connected = new AtomicBoolean(true);
		this.messagingConnector = messagingConnector;
		this.syncedProjects = new ConcurrentHashMap<String, ConnectedProject>();
		this.repositoryListeners = new ConcurrentLinkedDeque<>();
		
		this.messageHandlers = new ArrayList<IMessageHandler>(9);
		
		IMessageHandler getMetadataRequestHandler = new MessageHandler("getMetadataRequest") {
			@Override
			public void handle(String messageType, JSONObject message) {
				getMetadata(message);
			}
		};
		this.messagingConnector.addMessageHandler(getMetadataRequestHandler);
		this.messageHandlers.add(getMetadataRequestHandler);
	}
	
	public String getUsername() {
		return this.username;
	}

	public ConnectedProject getProject(IProject project) {
		return getProject(project.getName());
	}
	
	public ConnectedProject getProject(String projectName) {
		return this.syncedProjects.get(projectName);
	}

	public boolean isConnected(IProject project) {
		return isConnected(project.getName());
	}

	public boolean isConnected(String projectName) {
		Project project = this.repository.getProject(projectName);
		return this.repository.getSynchronizedProjects().contains(project);
	}

	public void addProject(IProject project) {
		this.repository.addProject(project.getName(), project.getLocationURI().getPath());
		notifyProjectConnected(project);
	}

	public void removeProject(IProject project) {
		this.repository.removeProject(project.getName());
		notifyProjectDisonnected(project);
	}
	
	public ConnectedProject[] getConnectedProjects() {
		return syncedProjects.values().toArray(
				new ConnectedProject[syncedProjects.size()]);
	}

	public void getMetadata(JSONObject request) {
		try {
			final String username = request.getString("username");
			final int callbackID = request.getInt("callback_id");
			final String sender = request.getString("requestSenderID");
			final String projectName = request.getString("project");
			final String resourcePath = request.getString("resource");

			ConnectedProject connectedProject = this.syncedProjects.get(projectName);
			if (this.username.equals(username) && connectedProject != null) {
				IProject project = connectedProject.getProject();
				IResource resource = project.findMember(resourcePath);

				JSONObject message = new JSONObject();
				message.put("callback_id", callbackID);
				message.put("requestSenderID", sender);
				message.put("username", this.username);
				message.put("project", projectName);
				message.put("resource", resourcePath);
				message.put("type", "marker");

				IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
				String markerJSON = toJSON(markers);
				JSONArray content = new JSONArray(markerJSON);
				message.put("metadata", content);

				messagingConnector.send("getMetadataResponse", message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void metadataChanged(IResourceDelta delta) {
		IProject project = delta.getResource().getProject();
		IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
		if (project != null && isConnected(project) && markerDeltas != null && markerDeltas.length > 0) {
			sendMetadataUpdate(delta.getResource());
		}
	}

	public void sendMetadataUpdate(IResource resource) {
		try {
			String project = resource.getProject().getName();
			String resourcePath = resource.getProjectRelativePath().toString();

			JSONObject message = new JSONObject();
			message.put("username", this.username);
			message.put("project", project);
			message.put("resource", resourcePath);
			message.put("type", "marker");

			IMarker[] markers = resource.findMarkers(null, true, IResource.DEPTH_INFINITE);
			String markerJSON = toJSON(markers);
			JSONArray content = new JSONArray(markerJSON);
			message.put("metadata", content);

			messagingConnector.send("metadataChanged", message);
		} catch (Exception e) {

		}
	}

	public String toJSON(IMarker[] markers) {
		StringBuilder result = new StringBuilder();
		boolean flag = false;
		result.append("[");
		for (IMarker m : markers) {
			if (flag) {
				result.append(",");
			}

			result.append("{");
			result.append("\"description\":" + JSONObject.quote(m.getAttribute("message", "")));
			result.append(",\"line\":" + m.getAttribute("lineNumber", 0));
			result.append(",\"severity\":\"" + (m.getAttribute("severity", IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR ? "error" : "warning")
					+ "\"");
			result.append(",\"start\":" + m.getAttribute("charStart", 0));
			result.append(",\"end\":" + m.getAttribute("charEnd", 0));
			result.append("}");

			flag = true;
		}
		result.append("]");
		return result.toString();
	}
	
	public void addRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}
	
	public void removeRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}
	
	protected void notifyProjectConnected(IProject project) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.projectConnected(project);
		}
	}

	protected void notifyProjectDisonnected(IProject project) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.projectDisconnected(project);
		}
	}
	
	protected void notifyResourceChanged(IResource resource) {
		for (IRepositoryListener listener : this.repositoryListeners) {
			listener.resourceChanged(resource);
		}
	}
	
	public void dispose() {
		connected.set(false);
		for (IMessageHandler messageHandler : messageHandlers) {
			messagingConnector.removeMessageHandler(messageHandler);
		}
		syncedProjects.clear();
	}

}
