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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flux.core.handlers.MetadataRequestHandler;
import org.eclipse.flux.core.handlers.ProjectRequestHandler;
import org.eclipse.flux.core.handlers.ProjectResponseHandler;
import org.eclipse.flux.core.handlers.ProjectsResponseHandler;
import org.eclipse.flux.core.handlers.ResourceChangedHandler;
import org.eclipse.flux.core.handlers.ResourceCreatedHandler;
import org.eclipse.flux.core.handlers.ResourceDeletedHandler;
import org.eclipse.flux.core.handlers.ResourceRequestHandler;
import org.eclipse.flux.core.handlers.ResourceResponseHandler;
import org.eclipse.flux.core.util.JSONUtils;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.RepositoryEvent;
import org.eclipse.flux.watcher.core.RepositoryEventBus;
import org.eclipse.flux.watcher.core.RepositoryListener;
import org.eclipse.flux.watcher.core.Resource;
import org.eclipse.flux.watcher.core.spi.Project;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class RepositoryAdapter {
    private static int GET_PROJECT_CALLBACK = "Repository - getProjectCallback".hashCode();
    private static int GET_RESOURCE_CALLBACK = "Repository - getResourceCallback".hashCode();

	private String username;

	private Collection<IRepositoryListener> repositoryListeners;

    private RepositoryEventBus repositoryEventBus;
	private Repository repository;
	private MessageConnector messageConnector;
	private Collection<IMessageHandler> messageHandlers;

	public RepositoryAdapter(MessageConnector messageConnector, Repository repository, String user) {
		this.repository = repository;
        this.repositoryEventBus = repository.repositoryEventBus();
		this.messageConnector = messageConnector;
		this.username = user;
		this.messageHandlers = new ArrayList<>();
		this.repositoryListeners = new ConcurrentLinkedDeque<>();
				
		IMessageHandler metadataRequestHandler = new MetadataRequestHandler(messageConnector, repository);
		this.messageConnector.addMessageHandler(metadataRequestHandler);
		this.messageHandlers.add(metadataRequestHandler);
		
		IMessageHandler projectsRequestHandler = new ProjectsResponseHandler(messageConnector, repository);
        this.messageConnector.addMessageHandler(projectsRequestHandler);
        this.messageHandlers.add(projectsRequestHandler);
        
        IMessageHandler projectRequestHandler = new ProjectRequestHandler(messageConnector, repository);
        this.messageConnector.addMessageHandler(projectRequestHandler);
        this.messageHandlers.add(projectRequestHandler);
        
        IMessageHandler projectResponseHandler = new ProjectResponseHandler(messageConnector, repository, GET_PROJECT_CALLBACK);
        this.messageConnector.addMessageHandler(projectResponseHandler);
        this.messageHandlers.add(projectResponseHandler);
        
        IMessageHandler resourceRequestHandler = new ResourceRequestHandler(messageConnector, repository);
        this.messageConnector.addMessageHandler(resourceRequestHandler);
        this.messageHandlers.add(resourceRequestHandler);
        
        IMessageHandler resourceResponseHandler = new ResourceResponseHandler(messageConnector, repository, GET_RESOURCE_CALLBACK);
        this.messageConnector.addMessageHandler(resourceResponseHandler);
        this.messageHandlers.add(resourceResponseHandler);
        
        IMessageHandler resourceCreatedHandler = new ResourceCreatedHandler(messageConnector, repository, GET_RESOURCE_CALLBACK);
        this.messageConnector.addMessageHandler(resourceCreatedHandler);
        this.messageHandlers.add(resourceCreatedHandler);
        
        IMessageHandler resourceChangedHandler = new ResourceChangedHandler(messageConnector, repository, GET_RESOURCE_CALLBACK);
        this.messageConnector.addMessageHandler(resourceChangedHandler);
        this.messageHandlers.add(resourceChangedHandler);
        
        IMessageHandler resourceDeletedHandler = new ResourceDeletedHandler(messageConnector, repository);
        this.messageConnector.addMessageHandler(resourceDeletedHandler);
        this.messageHandlers.add(resourceDeletedHandler);
        
        this.repositoryEventBus.addRepositoryListener(new RepositoryListener() {
            @Override
            public void onEvent(RepositoryEvent event) throws Exception {
                Project project = event.project();
                Resource resource = event.resource();
                switch (event.type()) {
                    case PROJECT_RESOURCE_CREATED:
                        JSONObject createdStoredMessage = new JSONObject();
                        createdStoredMessage.put("username", RepositoryAdapter.this.username);
                        createdStoredMessage.put("project", project.id());
                        createdStoredMessage.put("resource", resource.path());
                        createdStoredMessage.put("timestamp", resource.timestamp());
                        createdStoredMessage.put("hash", resource.hash());
                        createdStoredMessage.put("type", resource.type().name().toLowerCase());
                        RepositoryAdapter.this.messageConnector.send("resourceCreated", createdStoredMessage);
                        RepositoryAdapter.this.messageConnector.send("resourceStored", createdStoredMessage);
                        break;
                    case PROJECT_RESOURCE_MODIFIED:
                        JSONObject modifiedStoredMessage = new JSONObject();
                        modifiedStoredMessage.put("username", RepositoryAdapter.this.username);
                        modifiedStoredMessage.put("project", project.id());
                        modifiedStoredMessage.put("resource", resource.path());
                        modifiedStoredMessage.put("timestamp", resource.timestamp());
                        modifiedStoredMessage.put("hash", resource.hash());
                        RepositoryAdapter.this.messageConnector.send("resourceChanged", modifiedStoredMessage);
                        RepositoryAdapter.this.messageConnector.send("resourceStored", modifiedStoredMessage);
                        break;
                    case PROJECT_RESOURCE_DELETED:
                        JSONObject message = new JSONObject();
                        message.put("username", RepositoryAdapter.this.username);
                        message.put("project", project.id());
                        message.put("resource", resource.path());
                        message.put("timestamp", resource.timestamp());
                        RepositoryAdapter.this.messageConnector.send("resourceDeleted", message);
                        break;
                    default:
                        break;
                }
            }
        });
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
		sendProjectConnectedMessage(project.getName());
        syncConnectedProject(project.getName());
	}

	public void removeProject(IProject project) {
		this.repository.removeProject(project.getName());
		notifyProjectDisonnected(project);
		JSONObject message = new JSONObject();
        try {
            message.put("username", this.username);
            message.put("project", project.getName());
            messageConnector.send("projectDisconnected", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
			messageConnector.send(IMessageHandler.METADATA_CHANGED, message);
		} catch (Exception e) {

		}
	}
	
	public void addRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.add(listener);
	}
	
	public void removeRepositoryListener(IRepositoryListener listener) {
		this.repositoryListeners.remove(listener);
	}
	
	protected void syncConnectedProject(String projectName) {
        try {
            JSONObject message = new JSONObject();
            message.put("username", this.username);
            message.put("project", projectName);
            message.put("includeDeleted", true);
            message.put("callback_id", GET_PROJECT_CALLBACK);
            messageConnector.send("getProjectRequest", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void sendProjectConnectedMessage(String projectName) {
        try {
            JSONObject message = new JSONObject();
            message.put("username", this.username);
            message.put("project", projectName);
            messageConnector.send("projectConnected", message);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
	    for(IMessageHandler messageHandler : messageHandlers){
	        messageConnector.removeMessageHandler(messageHandler);
	    }
	}

}
