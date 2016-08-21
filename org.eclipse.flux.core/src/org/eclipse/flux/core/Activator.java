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

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Executors;

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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.config.SocketIOFluxConfig;
import org.eclipse.flux.core.internal.CloudSyncMetadataListener;
import org.eclipse.flux.core.util.ExceptionUtil;
import org.eclipse.flux.watcher.core.Credentials;
import org.eclipse.flux.watcher.core.Repository;
import org.eclipse.flux.watcher.core.RepositoryModule;
import org.eclipse.flux.watcher.fs.JDKProjectModule;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Martin Lippert
 * @author Miles Parker
 */
public class Activator extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.flux.core"; //$NON-NLS-1$

	private static final String CONNECTED_PROJECTS_ID = "connected.projects";

	// The shared instance
	private static Activator plugin;

	private MessageConnector messageConnector;
	private ChannelSwitcher channelSwitcher;
	
	private RepositoryAdapter repository;
	private LiveEditCoordinator liveEditCoordinator;
	private boolean lazyStart = false;
	
	private CloudSyncMetadataListener metadataListener;
	private IRepositoryListener repositoryListener;
	private IResourceChangeListener workspaceListener;
	
	private Repository fluxRepository;
	
	private final IChannelListener SERVICE_STARTER = new IChannelListener() {
		@Override
		public void connected(String userChannel) {
			if (!(lazyStart && MessageConstants.SUPER_USER.equals(userChannel))) {
				try {
					plugin.initCoreService(userChannel);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void disconnected(String userChannel) {
			if (!(lazyStart && MessageConstants.SUPER_USER.equals(userChannel))) {
				disposeCoreServices(userChannel);
			}
		}
	};

	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		
		String host = getHostUrl();
		String login = getUserId();
		String token = getUserToken();
		String lazyStartStr = System.getProperty("flux.lazyStart") == null ? System.getenv("FLUX_LAZY_START") : System.getProperty("flux.lazyStart");
		lazyStart = lazyStartStr != null && Boolean.valueOf(lazyStartStr);
		
		String channel = System.getProperty("flux.channel.id") == null ? System.getenv("FLUX_CHANNEL_ID") : System.getProperty("flux.channel.id");
		if (channel == null) {
			channel = login;
		}
		
		
		if (!host.isEmpty()) {
			this.messageConnector = new FluxClient(Executors.newFixedThreadPool(1)).connect(new SocketIOFluxConfig(host, login, token));
			this.channelSwitcher = new ChannelSwitcher(messageConnector);
			this.messageConnector.addChannelListener(SERVICE_STARTER);
			
			final String userChannel = lazyStart ? MessageConstants.SUPER_USER : channel;
			
			Injector injector = Guice.createInjector(new RepositoryModule(), new JDKProjectModule());
			fluxRepository = injector.getInstance(Repository.class);
			fluxRepository.addRemote(new URL(host), new Credentials(login, token));
			//Connecting to channel done asynchronously. To avoid blocking plugin state initialization.
			FluxClient.DEFAULT_INSTANCE.getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					try {
						channelSwitcher.switchToChannel(userChannel);
					} catch (Exception e) {
						log(e);
					}
				}
			});
		}
	}
	
	public static String getHostUrl() {
		String host = System.getProperty("flux-host") == null ? System.getenv("FLUX_HOST") : System.getProperty("flux-host");
		if (host == null) {
			host = InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(IPreferenceConstants.PREF_URL, "");
		}
		return host;
	}
	
	public static String getUserId() {
		String login = System.getProperty("flux.user.name") == null ? System.getenv("FLUX_USER_ID") : System.getProperty("flux.user.name");
		if (login == null) {
			login = InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(IPreferenceConstants.PREF_USER_ID, "");
		}
		return login;
	}
	
	public static String getUserToken() {
		String token = System.getProperty("flux.user.token") == null ? System.getenv("FLUX_USER_TOKEN") : System.getProperty("flux.user.token");
		if (token == null) {
			token = InstanceScope.INSTANCE.getNode(PLUGIN_ID).get(IPreferenceConstants.PREF_USER_TOKEN, "");
		}
		return token;
	}
	
	public static boolean isConnectionSettingsViaPreferences() {
		return System.getProperty("flux-host") == null
				&& System.getenv("FLUX_HOST") == null
				&& System.getProperty("flux.user.name") == null
				&& System.getenv("FLUX_USER_ID") == null
				&& System.getProperty("flux.user.token") == null
				&& System.getenv("FLUX_USER_TOKEN") == null;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (messageConnector != null) {
			messageConnector.disconnect();
		}
		plugin = null;
	}
	
	private void initCoreService(String userChannel) throws CoreException {
		repository = new RepositoryAdapter(messageConnector, fluxRepository, userChannel);
		liveEditCoordinator = new LiveEditCoordinator(fluxRepository);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		
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
				RepositoryAdapter repository = org.eclipse.flux.core.Activator.getDefault()
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
	
	public ChannelSwitcher getChannelSwitcher() {
		return channelSwitcher;
	}
	
	public MessageConnector getMessageConnector() {
		return messageConnector;
	}
	
	public RepositoryAdapter getRepository() {
		return repository;
	}
	
	public LiveEditCoordinator getLiveEditCoordinator() {
		return liveEditCoordinator;
	}

	public static void log(Throwable ex) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, ExceptionUtil.getMessage(ex), ex));
	}
	
	public boolean isLazyStart() {
		return lazyStart;
	}


}
