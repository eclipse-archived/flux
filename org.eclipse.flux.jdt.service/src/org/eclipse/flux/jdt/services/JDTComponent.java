/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *     IBM Corporation - converted to DS component
 *******************************************************************************/
package org.eclipse.flux.jdt.services;

import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.ChannelSwitcher;
import org.eclipse.flux.core.KeepAliveConnector;
import org.eclipse.flux.core.LiveEditCoordinator;
import org.eclipse.flux.core.RepositoryAdapter;
import org.eclipse.flux.core.ServiceDiscoveryConnector;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * This component connects Java development tools (JDT) services to the Flux message bus.
 */
public class JDTComponent {
	
	public static final String JDT_SERVICE_ID = "org.eclipse.flux.jdt";
	private static long WAIT_TIME_PERIOD = 100;

	private ServiceDiscoveryConnector discoveryConnector;
	private KeepAliveConnector keepAliveConnector;
	
	@Activate
	public void activate(final ComponentContext context) throws Exception {		
		final boolean lazyStart = org.eclipse.flux.core.Activator.getDefault().isLazyStart();
		
		final ChannelSwitcher channelSwitcher = org.eclipse.flux.core.Activator
				.getDefault().getChannelSwitcher();
		final MessageConnector messagingConnector = org.eclipse.flux.core.Activator
				.getDefault().getMessageConnector();
		
		if (messagingConnector != null) {
			new Thread() {

				@Override
				public void run() {
					
					String userChannel = channelSwitcher.getChannel();
					JdtChannelListener jdtChannelListener = new JdtChannelListener();
					for (; userChannel == null; userChannel = channelSwitcher
							.getChannel()) {
						try {
							sleep(WAIT_TIME_PERIOD);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					discoveryConnector = new ServiceDiscoveryConnector(channelSwitcher, messagingConnector, JDT_SERVICE_ID, lazyStart);
					if (lazyStart) {
						keepAliveConnector = new KeepAliveConnector(channelSwitcher, messagingConnector, JDT_SERVICE_ID);
					}
					
					jdtChannelListener.connected(userChannel);
					messagingConnector.addChannelListener(jdtChannelListener);
				}
				
			}.start();
					
		}
		
	}
	
	@Deactivate
	public synchronized void deactivate(final ComponentContext context) {
		if (discoveryConnector!=null) {
			discoveryConnector.dispose();
		}
		if (keepAliveConnector != null) {
			keepAliveConnector.dispose();
		}
	}

	/**
	 * Flux Channel listener for JDT service (supports lazy start option)
	 * 
	 * @author aboyko
	 *
	 */
	private class JdtChannelListener implements IChannelListener {
		
		private LiveEditUnits liveEditUnits;
		private ContentAssistService contentAssistService;
		private NavigationService navigationService;
		private RenameService renameService;
		private JavaDocService javadocService;
		private QuickAssistService quickAssistService;
		private InitializeServiceEnvironment initializer;

		@Override
		public void connected(String userChannel) {
			boolean lazyStart = org.eclipse.flux.core.Activator.getDefault().isLazyStart();
			if (lazyStart && MessageConstants.SUPER_USER.equals(userChannel)) {
				return;
			}
			MessageConnector messagingConnector = org.eclipse.flux.core.Activator
					.getDefault().getMessageConnector();
			RepositoryAdapter repository = org.eclipse.flux.core.Activator.getDefault()
					.getRepository();
			LiveEditCoordinator liveEditCoordinator = org.eclipse.flux.core.Activator
					.getDefault().getLiveEditCoordinator();

			this.liveEditUnits = new LiveEditUnits(messagingConnector,
					liveEditCoordinator, repository);
			this.contentAssistService = new ContentAssistService(messagingConnector, liveEditUnits);
			this.navigationService = new NavigationService(messagingConnector, liveEditUnits);
			this.renameService = new RenameService(messagingConnector, liveEditUnits);
			this.javadocService = new JavaDocService(messagingConnector, liveEditUnits);
			this.quickAssistService = new QuickAssistService(messagingConnector, liveEditUnits);
			
			String initJdtStr = System.getProperty("flux-initjdt") == null ? System.getenv("FLUX_INIT_JDT") : System.getProperty("flux-initjdt");
			if (initJdtStr != null && Boolean.valueOf(initJdtStr)) {
				this.initializer = new InitializeServiceEnvironment(
						messagingConnector, repository);
				initializer.start();
			}
		}

		@Override
		public void disconnected(String userChannel) {
			boolean lazyStart = org.eclipse.flux.core.Activator.getDefault().isLazyStart();
			if (lazyStart && MessageConstants.SUPER_USER.equals(userChannel)) {
				return;
			}
			liveEditUnits.dispose();
			contentAssistService.dispose();
			navigationService.dispose();
			renameService.dispose();
			javadocService.dispose();
			quickAssistService.dispose();
			if (initializer != null) {
				initializer.dispose();
			}
		}

	}
	
}
