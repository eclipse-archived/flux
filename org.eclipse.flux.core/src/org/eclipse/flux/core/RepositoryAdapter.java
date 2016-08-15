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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flux.core.listeners.MetadataRequestHandler;
import org.eclipse.flux.core.listeners.ResourceListener;
import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageType;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class RepositoryAdapter {

	private String username;

	private ConcurrentMap<String, ConnectedProject> syncedProjects;
	private Collection<IRepositoryListener> repositoryListeners;
		
	private Repository repository;

	public RepositoryAdapter(Repository repository, String user) {
		this.repository = repository;
		this.username = user;
		this.syncedProjects = new ConcurrentHashMap<String, ConnectedProject>();
		this.repositoryListeners = new ConcurrentLinkedDeque<>();
			
		repository.getMessageBus().addMessageHandler(new ResourceListener());
		repository.getMessageBus().addMessageHandler(new MetadataRequestHandler());
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
			JSONArray content = toJSON(markers);
			message.put("metadata", content);
			repository.getMessageBus().sendMessages(new FluxMessage(FluxMessageType.METADATA_CHANGED, message));
		} catch (Exception e) {

		}
	}

	public JSONArray toJSON(IMarker[] markers) throws JSONException{
		JSONArray objects = new JSONArray();
		for(IMarker marker : markers){
			JSONObject object = new JSONObject();
			object.put("description", marker.getAttribute("message", ""));
			object.put("line", marker.getAttribute("lineNumber", 0));
			switch(marker.getAttribute("severity", IMarker.SEVERITY_WARNING)){
				case IMarker.SEVERITY_WARNING:
					object.put("severity", marker.getAttribute("severity", "warning"));
					break;
				case IMarker.SEVERITY_ERROR:
					object.put("severity", marker.getAttribute("severity", "error"));
					break;
			}
			object.put("start", marker.getAttribute("charStart", 0));
			object.put("end", marker.getAttribute("charEnd", 0));
			objects.put(object);
		}
		return objects;
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
		syncedProjects.clear();
	}

}
