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

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.core.sync.FluxSystemSync;
import org.eclipse.flux.watcher.core.spi.Project;

/**
 * @author Martin Lippert
 */
public class RepositoryAdapter{
	private FluxSystemSync systemSync;

	private Collection<IRepositoryListener> repositoryListeners;

	public RepositoryAdapter(MessageConnector messageConnector, String user) {
	    this.systemSync = new FluxSystemSync(messageConnector, user);
		this.repositoryListeners = new ConcurrentLinkedDeque<>();
	}
	
	public String getUsername() {
		return systemSync.getUsername();
	}

	public ConnectedProject getProject(IProject project) {
		return getProject(project.getName());
	}
	
	public ConnectedProject getProject(String projectName) {
        return new ConnectedProject(systemSync.getWatcherProject(projectName));
    }

	public boolean isConnected(IProject project) {
		return isConnected(project.getName());
	}

	public boolean isConnected(String projectName) {
		return systemSync.isProjectConnected(projectName);
	}

	public void addProject(IProject project) {
	    this.systemSync.addProject(project.getName(), project.getLocationURI().getPath());
	    notifyProjectConnected(project);
	}

	public void removeProject(IProject project) {
	    this.systemSync.removeProject(project.getName());
	    notifyProjectDisonnected(project);
	}
	
	public ConnectedProject[] getConnectedProjects() {
	    Set<Project> projects = systemSync.getSynchronizedProjects();
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
			systemSync.sendMetadataUpdate(delta.getResource());
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
	    systemSync.dispose();
	}
}
