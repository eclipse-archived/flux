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
package org.eclipse.flux.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.flux.client.config.FluxConfig;

/**
 * The main entry point to the FluxClient API. Example usage:
 *
 *   //Connection to 'localhost' rabbitmq instance as fluxuser 'Bob':
 *   MessageConnector flux = FluxClient.DEFAULT_INSTANCE.connect(new RabbitMQFluxConfig("Bob"));
 *
 *   //Connection to 'remote' rabbitmq instance as fluxuser 'Bob':
 *   MessageConnector flux = FluxClient.DEFAULT_INSTANCE.connect(
 *   	new RabbitMQFluxConfig("Bob")
 *   	.setURI("...rabbitmq uri...")
 *   );
 *
 *   //Connection to 'remote' flux server via SocketIO webscokets as user "Bon"
 *   MessageConnector flux = FluxClient.DEFAULT_INSTANCE.connect(
 *   	new SocketIOFluxConfig("http://flux.cfapps.io:4443", "Bob", "...Bob access token...")
 *   );
 * 
 * @author Kris De Volder
 */
public class FluxClient {
	
	private final ExecutorService executor;
	
	public static final FluxClient DEFAULT_INSTANCE = new FluxClient(Executors.newCachedThreadPool());
		
	public FluxClient(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Connects to flux bus and blocks until a connection is established or failed.
	 */
	public MessageConnector connect(FluxConfig cc) throws Exception {
		return cc.connect(this);
	}

	public ExecutorService getExecutor() {
		return executor;
	}
	
}
