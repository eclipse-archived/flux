package org.eclipse.flux.client.impl;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.config.RabbitMQFluxConfig;
import org.json.JSONObject;

public class RabbitMQMessageConnector implements MessageConnector {

	private FluxClient client;
	private RabbitMQFluxConfig conf;

	public RabbitMQMessageConnector(FluxClient client,
			RabbitMQFluxConfig conf) {
		this.client = client;
		this.conf = conf;
	}

	@Override
	public void connectToChannel(String channel) {
		throw new Error("Not implemented");
	}

	@Override
	public void connectToChannelSync(String username) throws Exception {
		throw new Error("Not implemented");
	}

	@Override
	public void disconnectFromChannel(String channel) {
		throw new Error("Not implemented");
	}

	@Override
	public boolean isConnected(String channel) {
		throw new Error("Not implemented");
	}

	@Override
	public void send(String messageType, JSONObject message) {
		throw new Error("Not implemented");
	}

	@Override
	public void addMessageHandler(IMessageHandler messageHandler) {
		throw new Error("Not implemented");
	}

	@Override
	public void removeMessageHandler(IMessageHandler messageHandler) {
		throw new Error("Not implemented");
	}

	@Override
	public void addChannelListener(IChannelListener listener) {
		throw new Error("Not implemented");
	}

	@Override
	public void removeChannelListener(IChannelListener listener) {
		throw new Error("Not implemented");
	}

	@Override
	public void disconnect() {
		throw new Error("Not implemented");
	}

	@Override
	public boolean isConnected() {
		throw new Error("Not implemented");
	}

	@Override
	public FluxConfig getConfig() {
		return conf;
	}

}
