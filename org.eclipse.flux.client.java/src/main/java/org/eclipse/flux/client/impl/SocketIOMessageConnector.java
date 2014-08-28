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

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.IMessageHandler;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Connector to Flux web socket
 * 
 * @author aboyko
 * @author kdvolder
 */
public final class SocketIOMessageConnector implements MessageConnector {
	
	private SocketIO socket;
	private ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers = new ConcurrentHashMap<String, Collection<IMessageHandler>>();
	private ConcurrentLinkedQueue<IChannelListener> channelListeners = new ConcurrentLinkedQueue<IChannelListener>();
	final private String host;
	private String login;
	private String token;
	private Set<String> channels = Collections.synchronizedSet(new HashSet<String>());
	private AtomicBoolean connected = new AtomicBoolean(false);
	
	public SocketIOMessageConnector(final String host, final String login, String token) {
		this.host = host;
		this.login = login;
		this.token = token;
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			this.socket = createSocket(host);
			this.socket.connect(new IOCallback() {
				
				@Override
				public void on(String messageType, IOAcknowledge arg1, Object... data) {
					if (data.length == 1 && data[0] instanceof JSONObject) {
						handleIncomingMessage(messageType, (JSONObject)data[0]);
					}
				}
	
				@Override
				public void onConnect() {
					connected.compareAndSet(false, true);
					String[] channelsArray = channels.toArray(new String[channels.size()]);
					channels.clear();
					for (String channel : channelsArray) {
						connectToChannel(channel);
					}
				}
	
				@Override
				public void onDisconnect() {
					for (String channel : channels) {
						notifyChannelDisconnected(channel);
					}
					connected.compareAndSet(true, false);
				}
	
				@Override
				public void onError(SocketIOException ex) {
					ex.printStackTrace();					
					try {
						onDisconnect();						
						connected.compareAndSet(true, false);
						socket = createSocket(host);
						socket.connect(this);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public void onMessage(String arg0, IOAcknowledge arg1) {
					// Nothing
				}
	
				@Override
				public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
					// Nothing
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connectToChannel(final String channel) {
		if (isConnected() && channel != null && !channels.contains(channel)) {
			try {
				JSONObject message = new JSONObject();
				message.put("channel", channel);
				channels.add(channel);
				socket.emit("connectToChannel", new IOAcknowledge() {

					public void ack(Object... answer) {
						try {
							if (answer.length == 1
									&& answer[0] instanceof JSONObject
									&& ((JSONObject) answer[0])
											.getBoolean("connectedToChannel")) {
								notifyChannelConnected(channel);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				}, message);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void disconnectFromChannel(final String channel) {
		boolean removed = channels.remove(channel);
		if (isConnected() && removed) {
			try {
				JSONObject message = new JSONObject();
				message.put("channel", channel);
				socket.emit("disconnectFromChannel", new IOAcknowledge() {
	
					public void ack(Object... answer) {
						try {
							if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("disconnectedFromChannel")) {
								notifyChannelDisconnected(channel);
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				}, message);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private SocketIO createSocket(String host) throws MalformedURLException {
		SocketIO socket = new SocketIO(host);
		if (token != null) {
			socket.addHeader("X-flux-user-name", login);
			socket.addHeader("X-flux-user-token", token);
		}
		return socket;
	}
	
	private void handleIncomingMessage(String messageType, JSONObject message) {
		Collection<IMessageHandler> handlers = this.messageHandlers.get(messageType);
		if (handlers != null) {
			for (IMessageHandler handler : handlers) {
				try {
					if (handler.canHandle(messageType, message)) {
						handler.handle(messageType, message);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
	
	public void send(String messageType, JSONObject message) {
		socket.emit(messageType, message);
	}

	public boolean isConnected(String channel) {
		return isConnected() && channels.contains(channel);
	}
	
	public void addMessageHandler(IMessageHandler messageHandler) {
		this.messageHandlers.putIfAbsent(messageHandler.getMessageType(), new ConcurrentLinkedDeque<IMessageHandler>());
		this.messageHandlers.get(messageHandler.getMessageType()).add(messageHandler);
	}

	public void removeMessageHandler(IMessageHandler messageHandler) {
		Collection<IMessageHandler> handlers = this.messageHandlers.get(messageHandler.getMessageType());
		if (handlers != null) {
			handlers.remove(messageHandler);
		}
	}
	
	public void addChannelListener(IChannelListener listener) {
		this.channelListeners.add(listener);
	}
	
	public void removeChannelListener(IChannelListener listener) {
		this.channelListeners.remove(listener);
	}
	
	private void notifyChannelConnected(String userChannel) {
		for (IChannelListener listener : channelListeners) {
			try {
				listener.connected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private void notifyChannelDisconnected(String userChannel) {
		for (IChannelListener listener : channelListeners) {
			try {
				listener.disconnected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void disconnect() {
		socket.disconnect();
	}
	
	public String getHost() {
		return host;
	}
	
	public boolean isConnected() {
		return connected.get();
	}
	
}
