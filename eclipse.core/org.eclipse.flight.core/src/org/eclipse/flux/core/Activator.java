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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.flux.core.internal.CloudSyncMetadataListener;
import org.eclipse.flux.core.internal.CloudSyncResourceListener;
import org.eclipse.flux.core.internal.messaging.SocketIOMessagingConnector;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class Activator implements BundleActivator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.flux.core"; //$NON-NLS-1$

	private static final String CONNECTED_PROJECTS_ID = "connected.projects";

	// The shared instance
	private static Activator plugin;

	private IMessagingConnector messagingConnector;
	private Repository repository;
	private LiveEditCoordinator liveEditCoordinator;
	
	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		
		String username = System.getProperty("flux-username", "defaultuser");
		// TODO: change this username property to a preference and add authentication
		
		messagingConnector = new SocketIOMessagingConnector(username);
		repository = new Repository(messagingConnector, username);
		liveEditCoordinator = new LiveEditCoordinator(messagingConnector);
		
		CloudSyncResourceListener resourceListener = new CloudSyncResourceListener(repository);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

		CloudSyncMetadataListener metadataListener = new CloudSyncMetadataListener(repository);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(metadataListener, IResourceChangeEvent.POST_BUILD);

		getRepository()
				.addRepositoryListener(new IRepositoryListener() {
					@Override
					public void projectDisconnected(IProject project) {
						removeConnectedProjectPreference(project.getName());
					}

					@Override
					public void projectConnected(IProject project) {
						addConnectedProjectPreference(project.getName());
					}
				});

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResourceChangeListener listener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {
				if (event.getResource() instanceof IProject) {
					IResourceDelta delta = event.getDelta();
					if (delta == null) {
						return;
					}
					if (delta.getKind() == IResourceDelta.REMOVED) {
						IProject project = (IProject) event.getResource();
						removeConnectedProjectPreference(project.getName());
					} else if (delta.getKind() == IResourceDelta.CHANGED) {
						// TODO, we aren't handling project renaming yet
						// IProject project = (IProject) event.getResource();
						// String oldName =
						// delta.getMovedFromPath().lastSegment();
						// removeConnectedProjectPreference(oldName);
						// addConnectedProjectPreference(project.getName());
					}
				}
			}
		};
		workspace.addResourceChangeListener(listener);

		updateProjectConnections();

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
	}

	private void updateProjectConnections() throws CoreException {
		String[] projects = getConnectedProjectPreferences();
		for (String projectName : projects) {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(projectName);
			if (project.exists()) {
				if (!project.isOpen()) {
					project.open(null);
				}
				Repository repository = org.eclipse.flux.core.Activator.getDefault()
						.getRepository();
				repository.addProject(project);
			}
		}
	}

	private String[] getConnectedProjectPreferences() {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String[] projects = StringUtils.split(preferences.get(CONNECTED_PROJECTS_ID, ""),
				";");
		return projects;
	}

	private void addConnectedProjectPreference(String projectName) {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String currentPreferences = preferences.get(CONNECTED_PROJECTS_ID, "");
		String[] projects = StringUtils.split(currentPreferences, ";");
		for (String existingProjectName : projects) {
			if (existingProjectName.equals(projectName)) {
				return;
			}
		}
		currentPreferences += ";" + projectName;
		preferences.put(CONNECTED_PROJECTS_ID, currentPreferences);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// We really don't care that much..
		}
	}

	private void removeConnectedProjectPreference(String projectName) {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		String currentPreferences = preferences.get(CONNECTED_PROJECTS_ID, "");
		String[] projects = StringUtils.split(currentPreferences, ";");
		Collection<String> retainedProjects = new HashSet<String>();
		for (String existingProjectName : projects) {
			if (!existingProjectName.equals(projectName)) {
				retainedProjects.add(existingProjectName);
			}
		}
		String newPreferences = StringUtils.join(retainedProjects, ";");
		preferences.put(CONNECTED_PROJECTS_ID, newPreferences);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// We really don't care that much..
		}
	}
	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public IMessagingConnector getMessagingConnector() {
		return messagingConnector;
	}
	
	public Repository getRepository() {
		return repository;
	}
	
	public LiveEditCoordinator getLiveEditCoordinator() {
		return liveEditCoordinator;
	}

}
