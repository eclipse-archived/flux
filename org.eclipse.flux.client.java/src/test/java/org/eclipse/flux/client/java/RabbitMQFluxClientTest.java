package org.eclipse.flux.client.java;

import junit.framework.TestCase;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.config.RabbitMQFluxConfig;

public class RabbitMQFluxClientTest extends TestCase {
	
	private FluxClient client = FluxClient.DEFAULT_INSTANCE;

	public void testConnectAndDisconnect() throws Exception {
		MessageConnector conn = createConnection("testUser");
		conn.disconnect();
	}

	protected MessageConnector createConnection(String user) {
		return new RabbitMQFluxConfig(user).connect(client);
	}

}
