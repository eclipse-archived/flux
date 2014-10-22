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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.config.RabbitMQFluxConfig;
import org.eclipse.flux.client.config.UserPermissions;
import org.eclipse.flux.client.util.BasicFuture;
import org.eclipse.flux.client.util.Console;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitMQMessageConnector extends AbstractMessageConnector {

	//Note: a channel in 'flux is not the same thing as a channel in AMQP.
	//  What is called a 'channel' in flux is really more like a
	//  'routing key' in AMQP.
	// A 'channel' in AMQP is more like a 'session object' used to interact
	// with the AMQP API.

	/**
	 * Special user name routing key to deliver messages to all users.
	 * This name is internal only, client code uses '*' in username.
	 */
	private static final String EVERYONE = "$all$";

	private static Console console = Console.get(RabbitMQMessageConnector.class.getName());

	//private FluxClient client; // not used at the moment, so why store it?
	private RabbitMQFluxConfig conf;
	private ConnectionFactory factory;
	private UserPermissions permissions;

	private Connection connection;

	Channel channel;
	String inbox;
	String outbox;

	private DeliveryTypes deliveryTypes = DeliveryTypes.DEFAULTS;

	private Set<String> connectedChannels = new HashSet<String>();

	private ConnectionFactory connectionFactory() throws Exception {
		if (factory==null) {
			ConnectionFactory f = new ConnectionFactory();
			conf.applyTo(f);
			factory = f;
		}
		return factory;
	}

	public RabbitMQMessageConnector(FluxClient client, RabbitMQFluxConfig conf) throws Exception {
		super(client.getExecutor());
		this.conf = conf;
		this.connection = connectionFactory().newConnection();
		this.permissions = conf.permissions();
		this.channel = connection.createChannel();
		this.inbox = createInbox();
		this.outbox = createOutbox();
		receiveBroadcasts();
	}

	/**
	 * Subscribe to messages sent to EVERYONE.
	 */
	private void receiveBroadcasts() throws IOException {
		this.channel.queueBind(inbox, outbox, EVERYONE);
		console.log("Connected to topic "+EVERYONE);
	}

	private String createInbox() throws IOException {
		DeclareOk ok = this.channel.queueDeclare("", /*durable*/ false, /*exclusive*/false, /*autoDelete*/true, null);
		final String inbox = ok.getQueue();
		console.log("Inbox created: "+inbox);
		channel.basicConsume(inbox, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope,
					BasicProperties properties, byte[] body) throws IOException {
				try {
					JSONTokener tokener = new JSONTokener(new String(body, "utf8"));
					JSONObject obj = new JSONObject(tokener);
					if (!isSelfOriginated(obj)) {
						handleIncomingMessage(obj.getString("type"), obj.getJSONObject("data"));
					}
				} catch (Exception e) {
					console.log(e);
				}
			}

			/**
			 * Tests whether an incoming message originated from the same MessageConnector that
			 * is receiving it. (Such messages are skipped in keeping with how socketio does the same
			 * thing).
			 */
			private boolean isSelfOriginated(JSONObject obj) {
				try {
					String origin = obj.getString("origin");
					return inbox.equals(origin);
				} catch (Exception e) {
					console.log(e);
				}
				return false;
			}
		});
		return inbox;
	}

	private String createOutbox() throws IOException {
		String outbox = "flux"; //outbox queue/exchange name is the same 'flux' same for everyone.
		// rabbit mq routes messages placed in this queue via topic exchanges to
		// inboxes based on clients connectiong to a 'flux channels'.
		this.channel.exchangeDeclare(outbox, "topic");
		console.log("Outbox created");
		return outbox; 
	}

	private static String channelNameToTopicPattern(String fluxChannelName) {
		checkValidChannel(fluxChannelName);
		if (fluxChannelName.equals(MessageConstants.SUPER_USER)) {
			return "*";
		}
		return fluxChannelName;
	}

	private static void checkValidChannel(String fluxChannelName) {
		//Check that channel name contains no chars that have special meaning in AMQP routing patterns.
		int len = fluxChannelName.length();
		for (int i = 0; i < len; i++) {
			char c = fluxChannelName.charAt(i);
			if (c=='.' || c=='#' || c=='*') {
				throw new IllegalArgumentException("Flux channel name '"+fluxChannelName+"' contains a special character '"+c+"'");
			}
		}
	}

	/**
	 * Messages are routed based on the username in flux messages.
	 */
	public String usernameToRoutingKey(String name) {
		if (name.equals("*")) {
			return EVERYONE;
		}
		return name;
	}

	@Override
	public void connectToChannel(final String channel) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					connectToChannelSync(channel);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void connectToChannelSync(String channelName) throws Exception {
		boolean notifyNeeded = false;
		synchronized (this) {
			if (!isConnected(channelName)) {
				permissions.checkChannelJoin(channelName);
				String topic = channelNameToTopicPattern(channelName);
				this.channel.queueBind(this.inbox, this.outbox, topic);
				connectedChannels.add(channelName);
				notifyNeeded = true;
			}
		}
		if (notifyNeeded) {
			//Take care to call the listeners outside synch block to avoid potential deadlocks
			notifyChannelConnected(channelName);
		}
	}

	@Override
	public void disconnectFromChannelSync(String channelName) throws Exception {
		boolean notifyNeeded = false;
		synchronized (this) {
			if (isConnected(channelName)) {
				String topic = channelNameToTopicPattern(channelName);
				this.channel.queueUnbind(this.inbox, this.outbox, topic);
				connectedChannels.remove(channelName);
				notifyNeeded = true;
			}
		}
		if (notifyNeeded) {
			//Take care to call the listeners outside synch block to avoid potential deadlocks
			notifyChannelDisconnected(channelName);
		}
	}
	
	/**
	 * Asynchronous version of disconnect from Channel. 
	 */
	@Override
	public void disconnectFromChannel(final String channel) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					disconnectFromChannelSync(channel);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public synchronized boolean isConnected(String channel) {
		return connectedChannels.contains(channel);
	}

	@Override
	public void send(String messageType, JSONObject message) throws Exception {
		deliveryTypes.get(messageType).send(this, messageType, message);
	}

	@Override
	public void disconnect() {
		if (connection!=null) {
			try {
				connection.close();
			} catch (IOException e) {
				console.log(e);
			}
			connection = null;
		}
	}

	@Override
	public boolean isConnected() {
		throw new Error("Not implemented");
	}

	@Override
	public FluxConfig getConfig() {
		return conf;
	}

	/**
	 * Encode a message into byte array, ready to send over the wire.
	 */
	byte[] encode(String messageType, JSONObject data) throws Exception {
		JSONObject message = new JSONObject()
			.put("type", messageType)
			.put("origin", inbox) //needed to avoid delivering broadcasts to 'self'
			.put("data", data);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		OutputStreamWriter out= new OutputStreamWriter(bytes, "utf8");
		try {
			message.write(out);
		} finally {
			out.close();
		}
		return bytes.toByteArray();
	}

}
