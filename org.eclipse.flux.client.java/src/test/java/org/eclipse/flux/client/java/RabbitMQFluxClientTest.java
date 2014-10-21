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
package org.eclipse.flux.client.java;

import static org.eclipse.flux.client.MessageConstants.USERNAME;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.config.RabbitMQFluxConfig;
import org.eclipse.flux.client.util.BasicFuture;
import org.json.JSONObject;

public class RabbitMQFluxClientTest extends AbstractFluxClientTest {
	
	private static final String USER_BOB = "Bob";
	private static final String USER_ALICE = "Alice";
	
	private FluxClient client = FluxClient.DEFAULT_INSTANCE;
	
	public void testConnectAndDisconnect() throws Exception {
		MessageConnector conn = createConnection(USER_BOB);
		conn.disconnect();
	}
	
	public void testSendAndReceive() throws Exception {
		final Process<Void> sender = new Process<Void>(USER_BOB) {
			protected Void execute() throws Exception {
				send("bork", new JSONObject()
					.put(USERNAME, USER_BOB)
					.put("msg", "Hello")
				);
				return null;
			}

		};
		
		final Process<String> receiver = new Process<String>(USER_BOB) {
			protected String execute() throws Exception {
				BasicFuture<JSONObject> msg = areceive("bork");
				sender.start();
				return msg.get().getString("msg");
			}
		};

		receiver.start();	//only start receiver (not sender).
							// receiver should start sender at the right time 
							// to avoid race condition between them.
		await(sender, receiver);
		assertEquals("Hello", receiver.result.get());
	}
	
	protected MessageConnector createConnection(String user) throws Exception {
		return new RabbitMQFluxConfig(user).connect(client);
	}
	
	//TODO: user can receive messages sent to the '*' user.
	//TODO: super user receives all messages
	//TODO: normal user only receives messages from its subscribed channel 
	//TODO: normal user can only send messages to their own channel.
}

