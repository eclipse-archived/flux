package org.eclipse.flux.client;

import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.impl.SocketIOFluxClient;

public interface FluxClient {
	
	public static final FluxClient DEFAULT_INSTANCE = new SocketIOFluxClient();
	
	/**
	 * Deprecated, please use connect(ConnectionConfig) instead.
	 */
	@Deprecated
	MessageConnector connect(String host, String login, String token);
	
	/**
	 * Connects to flux bus and blocks until a connection is established or failed.
	 */
	MessageConnector connect(FluxConfig cc);
	
}
