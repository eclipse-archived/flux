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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flux.core.handlers.MetadataRequestHandler;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.core.handlers.EclipseResourceResponseHandler;
import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageBus;
import org.eclipse.flux.watcher.core.FluxMessageType;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class RepositoryAdapter {

	private String username;

	private Collection<IRepositoryListener> repositoryListeners;
		
	private Repository repository;
	private FluxMessageBus messageBus;

	public RepositoryAdapter(Repository repository, String user) {
		this.repository = repository;
		this.messageBus = repository.getMessageBus();
		this.username = user;
		this.repositoryListeners = new ConcurrentLinkedDeque<>();
			
		messageBus.addMessageHandler(new EclipseResourceResponseHandler());
		messageBus.addMessageHandler(new MetadataRequestHandler());
	}
	
	public FluxMessageBus getMessageBus(){
		return this.messageBus;
	}
	
	public String getUsername() {
		return this.username;
	}

	public ConnectedProject getProject(IProject project) {
		return getProject(project.getName());
	}
	
	public ConnectedProject getProject(String projectName) {
		return new ConnectedProject(repository.getProject(projectName));
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
	    Set<Project> projects = repository.getSynchronizedProjects();
	    Set<ConnectedProject> connectedProjects = new HashSet<>();
	    for(Project project : projects){
	        connectedProjects.add(new ConnectedProject(project));
	    }
		return connectedProjects.toArray(new ConnectedProject[connectedProjects.size()]);
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
			JSONArray content = JSONUtils.toJSON(markers);
			message.put("metadata", content);
			messageBus.sendMessages(new FluxMessage(FluxMessageType.METADATA_CHANGED, message));
		} catch (Exception e) {

		}
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
	}

}
