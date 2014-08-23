/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. and others.
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

import org.eclipse.flux.core.IMessagingConnector;
import org.eclipse.flux.core.ServiceDiscoveryConnector;
import org.eclipse.flux.core.KeepAliveConnector;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * This component connects Java development tools (JDT) services to the Flux message bus.
 */
public class JDTComponent {
	
	private static final String JDT_SERVICE_ID = "org.eclipse.flux.jdt";
	private ServiceDiscoveryConnector discoveryConnector;
	private KeepAliveConnector keepAliveConnector;
	
	private static JDTComponent instance = null;
	
	static JDTComponent getInstance() {
		return instance;
	}
	
	@Activate
	public void activate(final ComponentContext context) throws Exception {
		instance = this;
		
		String lazyStartStr = System.getProperty("flux.jdt.lazyStart") == null ? System.getenv("FLUX_LAZY_START") : System.getProperty("flux.jdt.lazyStart");
		boolean lazyStart = lazyStartStr != null && Boolean.valueOf(lazyStartStr);
		
		String login = System.getProperty("flux.user.name") == null ? System.getenv("FLUX_USER_ID") : System.getProperty("flux.user.name");
		if (login == null) {
			login = "defaultuser";
		}
		
		String token = System.getProperty("flux.user.token") == null ? System.getenv("FLUX_USER_TOKEN") : System.getProperty("flux.user.token");
		
		String host = System.getProperty("flux-host") == null ? System.getenv("FLUX_HOST") : System.getProperty("flux-host");
		if (host == null) {
			host = "http://localhost:3000";
		}
		
		String channel = System.getProperty("flux.channel.id") == null ? System.getenv("FLUX_CHANNEL_ID") : System.getProperty("lux.channel.id");
		if (channel == null) {
			channel = login;
		}
		
		org.eclipse.flux.core.Activator.getDefault().startService(host, login, token, !lazyStart);
		
		final IMessagingConnector messagingConnector = org.eclipse.flux.core.Activator
			.getDefault().getMessagingConnector();
		
		
		
		messagingConnector.connectChannel(channel);
		
		while (!messagingConnector.isChannelConnected()) {
			Thread.sleep(500);
		}
		
		discoveryConnector = new ServiceDiscoveryConnector(messagingConnector, channel, JDT_SERVICE_ID, lazyStart);
		if (lazyStart) {
			keepAliveConnector = new KeepAliveConnector(messagingConnector, channel, JDT_SERVICE_ID);
		}
		
	}
	
	public KeepAliveConnector getKeepAliveConnector() {
		return keepAliveConnector;
	}
	
	@Deactivate
	public void deactivate(final ComponentContext context) {
		if (discoveryConnector!=null) {
			discoveryConnector.dispose();
		}
		if (keepAliveConnector != null) {
			keepAliveConnector.dispose();
		}
		instance = null;
	}
	
}
