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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.config.SocketIOFluxConfig;
import org.eclipse.flux.client.util.BasicFuture;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Connector to Flux web socket
 * 
 * @author aboyko
 * @author kdvolder
 */
public final class SocketIOMessageConnector extends AbstractMessageConnector {
	
	/**
	 * Time in milliseconds a connectToChannelSynch call will wait before timing out.
	 */
	private static final long CONNECT_TO_CHANNEL_TIMEOUT = 15000;
	
	private SocketIO socket;
	private final SocketIOFluxConfig conf;
	private Set<String> channels = Collections.synchronizedSet(new HashSet<String>());
	private AtomicBoolean isConnected = new AtomicBoolean(false);
	
	public SocketIOMessageConnector(SocketIOFluxConfig conf, ExecutorService executor) {
		super(executor);
		this.conf = conf;
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			this.socket = createSocket();
			final BasicFuture<Void> connectedFuture = new BasicFuture<Void>();
			this.socket.connect(new IOCallback() {
				
				@Override
				public void on(String messageType, IOAcknowledge arg1, Object... data) {
					if (data.length == 1 && data[0] instanceof JSONObject) {
						handleIncomingMessage(messageType, (JSONObject)data[0]);
					}
				}
	
				@Override
				public void onConnect() {
					isConnected.compareAndSet(false, true);
					String[] channelsArray = channels.toArray(new String[channels.size()]);
					for (String channel : channelsArray) {
						connectToChannel(channel);
					}
					connectedFuture.resolve(null);
				}
	
				@Override
				public void onDisconnect() {
					System.out.println("Socket disconnected: "+socket);
					for (String channel : channels) {
						notifyChannelDisconnected(channel);
					}
					isConnected.compareAndSet(true, false);
				}
	
				@Override
				public void onError(SocketIOException ex) {
					connectedFuture.reject(ex);
					ex.printStackTrace();
					try {
						onDisconnect();						
						isConnected.compareAndSet(true, false);
						socket = createSocket();
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
			connectedFuture.get();
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Deprecated, please use connectToChannelSynch('myChannel') to
	 * connect to channel synchronously and avoid common bugs of the
	 * type 'oops I sent messages before the channel was connected'.
	 * <p>
	 * Also consider catching exceptions connectToChannelSynch('myChannel')
	 * might throw if it fails to connect to the channel. 
	 */
	@Deprecated
	public void connectToChannel(final String channel) {
		try {
			connectToChannel(channel, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void connectToChannelSync(final String channel) throws Exception {
		connectToChannel(channel, true);
	}
	
	private void connectToChannel(final String channel, final boolean sync) throws Exception {
		//TODO: suspected to buggy around tracking 'connected' stated for channels. 
		// especially in asynch case. The connected list of channels is immediately changed
		// even for asynch connection request. This means isConnected(channel) will already
		// return true even when channel connection is still being established.
		
		// One way to fix this problem is remove support for asynch connectToChannel entirely.
		// It really only serves to make for buggy code and provides there's not much reason
		// to use it if a synched mechanism is available.
		
		System.out.println("Connecting to Channel: "+channel);
		if (!isConnected()) {
			throw new IllegalStateException("Cannot connect to channel. Not connected to socket.io");
		}
		if (channel==null) {
			throw new IllegalArgumentException("Channel name should not be null");
		}
		final BasicFuture<Void> connectedFuture = sync ? new BasicFuture<Void>() : null;
		
// Commented out because this gets called to 'reconnect' to socketio after an error
// and in that case it already has channel in the channels list, but not actually connected
// yet.
//		if (!channels.contains(channel)) {
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
								if (sync) {
									connectedFuture.resolve(null);
								}
							} else {
								//TODO: add a better explanation?
								connectedFuture.reject(new IOException("Couldn't connect to channel "+channel));
							}
						} catch (Exception e) {
							e.printStackTrace();
							if (sync) {
								connectedFuture.reject(e);
							}
						}
					}

				}, message);
			} catch (JSONException e) {
				if (sync) {
					connectedFuture.reject(e);
				}
				e.printStackTrace();
			}
//		} else {
//			System.out.println("Skipping channel connect "+channel+" Already connected");
//		}
		if (sync) {
			connectedFuture.setTimeout(CONNECT_TO_CHANNEL_TIMEOUT);
			connectedFuture.get();
		}
	}
	
	@Override
	public void disconnectFromChannel(final String channel) {
		//TODO: this method is expected to be buggy w.r.t to updating channels connected state.
		// i.e. it immediately remoces channel from 'channels' but the operation is asyncronous.
		// This means that isConnected(channel) will return false even though the channel may
		// still be connected.
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
	
	@Override
	public void disconnectFromChannelSync(String channelName) throws Exception {
		//TODO: implement this
		throw new Error("Not implemented");
	}
	
	private SocketIO createSocket() throws MalformedURLException {
		System.out.println("Creating websocket to: "+conf.getHost());
		SocketIO socket = new SocketIO(conf.getHost());
		if (conf.getToken() != null) {
			socket.addHeader("X-flux-user-name", conf.getUser());
			socket.addHeader("X-flux-user-token", conf.getToken());
		}
		System.out.println("Created websocket: "+socket);
		return socket;
	}
	
	public void send(String messageType, JSONObject message) {
		socket.emit(messageType, message);
	}

	public boolean isConnected(String channel) {
		return isConnected() && channels.contains(channel);
	}
	
	public void disconnect() {
		socket.disconnect();
	}
	
	public boolean isConnected() {
		return isConnected.get();
	}

	@Override
	public FluxConfig getConfig() {
		return conf;
	}

}
