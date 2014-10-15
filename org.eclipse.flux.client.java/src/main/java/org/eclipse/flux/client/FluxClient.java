package org.eclipse.flux.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.flux.client.config.FluxConfig;

public class FluxClient {
	
	private final ExecutorService executor;
	
	public static final FluxClient DEFAULT_INSTANCE = new FluxClient(Executors.newCachedThreadPool());
		
	public FluxClient(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Connects to flux bus and blocks until a connection is established or failed.
	 */
	public MessageConnector connect(FluxConfig cc) {
		return cc.connect(this);
	}

	public ExecutorService getExecutor() {
		return executor;
	}
	
}
