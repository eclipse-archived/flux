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
package org.eclipse.flux.ui.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.flux.core.IChannelListener;
import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.IRepositoryListener;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.Repository;
import org.eclipse.flux.ui.integration.handlers.LiveEditConnector;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;

/**
 * Waits for connected channel and once there is a channel connected initializes
 * data structures supporting Flux message system. Adds a channel connection
 * listener to initialize and dispose flux related data structures.
 * 
 * @author aboyko
 *
 */
public class UiStartup implements IStartup {
	
	private static long WAIT_TIME_PERIOD = 100;

	@Override
	public void earlyStartup() {
		IMessagingConnector messagingConnector = org.eclipse.flux.core.Activator
				.getDefault().getMessagingConnector();

		UiChannelListener uiChannelListener = new UiChannelListener();
		
		String userChannel = messagingConnector.getChannel();
		for (; userChannel == null; userChannel = messagingConnector
				.getChannel()) {
			try {
				Thread.sleep(WAIT_TIME_PERIOD);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		uiChannelListener.connected(userChannel);
		messagingConnector.addChannelListener(uiChannelListener);
	}
	
	private class UiChannelListener implements IChannelListener {
		
		private LiveEditConnector liveEditConnector = null;
		
		private IRepositoryListener repositoryListener = new IRepositoryListener() {
			@Override
			public void projectDisconnected(IProject project) {
				updateProjectLabel(project);
			}

			@Override
			public void projectConnected(IProject project) {
				updateProjectLabel(project);
			}

			@Override
			public void resourceChanged(IResource resource) {
				// nothing
			}
		};

		@Override
		public void connected(String userChannel) {
			Repository repository = org.eclipse.flux.core.Activator
					.getDefault().getRepository();
			
			repository.addRepositoryListener(repositoryListener);

			if (Boolean.getBoolean("flux-eclipse-editor-connect")) {
				LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator
						.getDefault().getLiveEditCoordinator();
				liveEditConnector = new LiveEditConnector(liveEditCoordinator, repository);
			}
		}

		@Override
		public void disconnected(String userChannel) {
			org.eclipse.flux.core.Activator
				.getDefault().getRepository().removeRepositoryListener(repositoryListener);
			if (liveEditConnector != null) {
				liveEditConnector.dispose();
			}
		}

	}

	private static void updateProjectLabel(final IProject project) {
		final CloudProjectDecorator projectDecorator = CloudProjectDecorator
				.getInstance();
		if (projectDecorator != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					projectDecorator
							.fireLabelProviderChanged(new LabelProviderChangedEvent(
									projectDecorator, project));
				}
			});
		}
	}

}
