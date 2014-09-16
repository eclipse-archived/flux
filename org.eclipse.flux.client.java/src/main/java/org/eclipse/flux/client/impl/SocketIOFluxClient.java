package org.eclipse.flux.client.impl;

import java.util.concurrent.ExecutorService;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.config.FluxConfig;

public class SocketIOFluxClient implements FluxClient {
	
	private ExecutorService executor;

	public SocketIOFluxClient(ExecutorService executor) {
		this.executor = executor;
	}
	
	@Override
	public MessageConnector connect(String host, String login, String token) {
		return new SocketIOMessageConnector(host, login, token, executor);
	}

	@Override
	public MessageConnector connect(FluxConfig cc) {
		return connect(cc.getHost(), cc.getUser(), cc.getToken());
	}

}
