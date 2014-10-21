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
package org.eclipse.flux.client.impl;

import org.json.JSONObject;
import static org.eclipse.flux.client.MessageConstants.*;

/**
 * In order to be deliverable a messageType must be associated with a DeliveryType. 
 * The DeliveryType defines how messages are delivered/routed 
 * using RabbitMQ topics, exchanges etc.
 */
public abstract class DeliveryType {
	
	//TODO: DeliveryTypes are not specific to RabbitMQ, yet the implementation of this class is,
	// 
	// The reason why this is ok for now is that SocketIO based client does not directly have to deal with
	// delivery types as these are handled entirely on the (node.)server side.
	
	private String name;
	public DeliveryType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Requests are broadcast in a similar fashion to 'broadcast' the difference is that
	 * request must add enough information (metadata) to the message to allow the
	 * recipient to send back a targetted response.
	 */
	public static final DeliveryType REQUEST = new DeliveryType("REQUEST") {
		@Override
		public void send(RabbitMQMessageConnector connector, String messageType, JSONObject data) throws Exception {
			String outbox = connector.outbox;
			data.put(REQUEST_SENDER_ID, connector.inbox);
			//logMsg("rabbit ["+ self.inbox +"] <= ", type, data);
			connector.channel.basicPublish(outbox, connector.usernameToRoutingKey(data.getString(USERNAME)),
				null, connector.encode(messageType, data)
			);
		}
	};
	
	public abstract void send(RabbitMQMessageConnector connector, String messageType, JSONObject message) throws Exception;

	/**
	 * Broadcast delivery sends a message to all connected MessageConnectors according to the
	 * user field in the message data (i.e. the message is broadcast within the user's channel.
	 */
	public static final DeliveryType BROADCAST = new DeliveryType("BROADCAST") {

		@Override
		public void send(RabbitMQMessageConnector connector, String type, JSONObject data) throws Exception {
			String outbox = connector.outbox;
			data.put("senderID", connector.inbox);
			connector.channel.basicPublish(outbox, connector.usernameToRoutingKey(data.getString(USERNAME)),
				null, connector.encode(type, data)
			);
		}
	};
	
	
	/**
	 * A response is delivered back directly to the specific MessageConnector from where the corresponding 
	 * request originated.
	 */
	public static final DeliveryType RESPONSE = new DeliveryType("RESPONSE") {
		
		@Override
		public void send(RabbitMQMessageConnector connector, String type, JSONObject data) throws Exception {
			data.put(RESPONSE_SENDER_ID, connector.inbox);
			//Deliver directly to inbox of the requester
			connector.channel.basicPublish("", data.getString(REQUEST_SENDER_ID), null,
				connector.encode(type, data)
			);
		};
	};
	
}
