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
package org.eclipse.flux.client.config;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.impl.RabbitMQMessageConnector;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQFluxConfig extends AbstractFluxConfig {

	/**
	 * Create default RabbitMQFluxConfig to rabbit mq instance on localhost
	 */
	public RabbitMQFluxConfig(String fluxUser) {
		super(fluxUser);
	}
	
	/**
	 * URI to configure rabbitmq connection factory. When running in deployed environment like cloudfoundry this
	 * will be provided somehow by binding the app to an AMQP service. In localhost deployments this will be null.
	 */
	private String uri = null;

	@Override
	public MessageConnector connect(FluxClient fluxClient) throws Exception {
		return new RabbitMQMessageConnector(fluxClient, this);
	}

	@Override
	public SocketIOFluxConfig toSocketIO() {
		throw new Error("Not implemented");
	}

	public RabbitMQFluxConfig setUri(String uri) {
		this.uri = uri;
		return this;
	}
	
	/**
	 * Configure the AMQP ConnectionFactory with information from this RabbitMQFluxConfig
	 */
	public void applyTo(ConnectionFactory f) throws Exception {
		if (uri!=null) {
			f.setUri(uri);
		} else {
			f.setHost("localhost");
		}
	}

}
