package org.eclipse.flux.client;

import org.eclipse.flux.client.impl.SocketIOFluxClient;

public interface FluxClient {
	
	public static final FluxClient DEFAULT_INSTANCE = new SocketIOFluxClient();
	
	MessageConnector connect(String host, String login, String token);
	
}
