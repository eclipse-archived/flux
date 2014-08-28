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
import org.eclipse.core.resources.IResource;
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

	private SocketIOMessagingConnector messagingConnector;
	private Repository repository;
	private LiveEditCoordinator liveEditCoordinator;
	private boolean lazyStart = false;
	
	private CloudSyncResourceListener resourceListener;
	private CloudSyncMetadataListener metadataListener;
	private IRepositoryListener repositoryListener;
	private IResourceChangeListener workspaceListener;
	
	private final IChannelListener SERVICE_STARTER = new IChannelListener() {
		@Override
		public void connected(String userChannel) {
			if (!(lazyStart && Constants.SUPER_USER.equals(userChannel))) {
				try {
					plugin.initCoreService(userChannel);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void disconnected(String userChannel) {
			if (!(lazyStart && Constants.SUPER_USER.equals(userChannel))) {
				disposeCoreServices(userChannel);
			}
		}
	};
	
	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		
		String login = System.getProperty("flux.user.name") == null ? System.getenv("FLUX_USER_ID") : System.getProperty("flux.user.name");
		if (login == null) {
			login = "defaultuser";
		}
		
		String token = System.getProperty("flux.user.token") == null ? System.getenv("FLUX_USER_TOKEN") : System.getProperty("flux.user.token");
		
		String host = System.getProperty("flux-host") == null ? System.getenv("FLUX_HOST") : System.getProperty("flux-host");
		if (host == null) {
			host = "http://localhost:3000";
		}
		
		String lazyStartStr = System.getProperty("flux.lazyStart") == null ? System.getenv("FLUX_LAZY_START") : System.getProperty("flux.lazyStart");
		lazyStart = lazyStartStr != null && Boolean.valueOf(lazyStartStr);
		
		String channel = System.getProperty("flux.channel.id") == null ? System.getenv("FLUX_CHANNEL_ID") : System.getProperty("flux.channel.id");
		if (channel == null) {
			channel = login;
		}
		
		this.messagingConnector = new SocketIOMessagingConnector(host, login, token);
		this.messagingConnector.addChannelListener(SERVICE_STARTER);
		
		final String userChannel = lazyStart ? Constants.SUPER_USER : channel;
		messagingConnector.addConnectionListener(new IConnectionListener() {
		
			@Override
			public void connected() {
				messagingConnector.removeConnectionListener(this);
				messagingConnector.connectChannel(userChannel);
			}
		
			@Override
			public void disconnected() {
				// nothing
			}
			
		});
		messagingConnector.connect();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (messagingConnector != null) {
			messagingConnector.disconnect();
		}
		plugin = null;
	}
	
	private void initCoreService(String userChannel) throws CoreException {
		repository = new Repository(messagingConnector, userChannel);
		liveEditCoordinator = new LiveEditCoordinator(messagingConnector);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		
		resourceListener = new CloudSyncResourceListener(repository);
		workspace.addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

		metadataListener = new CloudSyncMetadataListener(repository);
		workspace.addResourceChangeListener(metadataListener, IResourceChangeEvent.POST_BUILD);
		
		this.repositoryListener = new IRepositoryListener() {
			@Override
			public void projectDisconnected(IProject project) {
				removeConnectedProjectPreference(project.getName());
			}

			@Override
			public void projectConnected(IProject project) {
				addConnectedProjectPreference(project.getName());
			}

			@Override
			public void resourceChanged(IResource resource) {
				// nothing
			}
		};

		getRepository().addRepositoryListener(repositoryListener);

		workspaceListener = new IResourceChangeListener() {
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
		workspace.addResourceChangeListener(workspaceListener);

		updateProjectConnections();
	}
	
	private void disposeCoreServices(String userChannel) {
		if (userChannel.equals(repository.getUsername())) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.removeResourceChangeListener(workspaceListener);
			workspace.removeResourceChangeListener(resourceListener);
			workspace.removeResourceChangeListener(metadataListener);
			repository.removeRepositoryListener(repositoryListener);
			liveEditCoordinator.dispose();
			repository.dispose();
		}
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

	public static void log(Throwable ex) {
		ex.printStackTrace();
	}
	
	public boolean isLazyStart() {
		return lazyStart;
	}
	
}
