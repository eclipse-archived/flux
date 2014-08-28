package org.eclipse.flux.client.impl;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;

public class SocketIOFluxClient implements FluxClient {

	@Override
	public MessageConnector connect(String host, String login, String token) {
		return new SocketIOMessageConnector(host, login, token);
	}

}
