package org.eclipse.flux.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.impl.SocketIOFluxClient;

public interface FluxClient {
	
	//TODO: this executor service instance doesn't really belong in here. Should be injected somehow.
	public static final ExecutorService executor = Executors.newCachedThreadPool();
	public static final FluxClient DEFAULT_INSTANCE = new SocketIOFluxClient(executor);
	
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
