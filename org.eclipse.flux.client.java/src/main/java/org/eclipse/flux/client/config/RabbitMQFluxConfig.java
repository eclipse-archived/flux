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
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.impl.RabbitMQMessageConnector;
import org.eclipse.flux.client.util.Console;
import org.eclipse.flux.client.util.JSON;
import org.json.JSONArray;
import org.json.JSONObject;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQFluxConfig extends AbstractFluxConfig {
	
	private static Console console = Console.get(RabbitMQFluxConfig.class.getName());

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
	
	/**
	 * Optionally a socketIO config can be associated with a RabbitMQConfig. This is to support use cases where
	 * a web app on the server side uses RabbitMQ but need to use websocket for client-side js code.
	 * <p>
	 * The web-app will need to pass on socketio config infos to the client-side code somehow.
	 */
	private SocketIOFluxConfig socketIOConfig = null;

	@Override
	public MessageConnector connect(FluxClient fluxClient) throws Exception {
		return new RabbitMQMessageConnector(fluxClient, this);
	}

	@Override
	public SocketIOFluxConfig toSocketIO() {
		return socketIOConfig;
	}

	public RabbitMQFluxConfig setUri(String uri) {
		this.uri = uri;
		return this;
	}
	
	public RabbitMQFluxConfig setSocketIOConf(SocketIOFluxConfig socketIOConfig) {
		this.socketIOConfig = socketIOConfig;
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
	
	public static String rabbitUrl() throws Exception {
		String vcapServices = System.getenv("VCAP_SERVICES");
		if (vcapServices!=null) {
			JSONObject svcInfo = JSON.parse(vcapServices);
			console.log("VCAP_SERVICES = "+ svcInfo);
			for (String label: JSONObject.getNames(svcInfo)) {
				JSONArray svcs = svcInfo.getJSONArray(label);
				for (int index = 0; index < svcs.length(); index++) {
					String uri = svcs.getJSONObject(index)
							.getJSONObject("credentials")
							.getString("uri");
					if (uri.contains("amqp")) {
						console.log("rabbit url = "+ uri);
						return uri;
					}
				}
			}
			throw new Error("Running on CF requires that you bind a amqp service to this app");
		} else {
			return "amqp://localhost";
		}
	}
	

	public static FluxConfig superConfig() throws Exception {
		return new RabbitMQFluxConfig(MessageConstants.SUPER_USER)
			.setUri(rabbitUrl());
	}

}
