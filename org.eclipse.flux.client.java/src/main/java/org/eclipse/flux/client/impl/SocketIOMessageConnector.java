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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.config.SocketIOFluxConfig;
import org.eclipse.flux.client.util.BasicFuture;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.IO.Options;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter.Listener;
import io.socket.engineio.client.Transport;

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
	
	private Options opts;
	private Socket socket;
	private final SocketIOFluxConfig conf;
	private Set<String> channels = Collections.synchronizedSet(new HashSet<String>());
	private AtomicBoolean isConnected = new AtomicBoolean(false);
	
	public SocketIOMessageConnector(SocketIOFluxConfig conf, ExecutorService executor) {
		super(executor);
		this.conf = conf;
		try {
			final BasicFuture<Void> connectedFuture = new BasicFuture<Void>();
			System.out.println("Creating websocket to: "+conf.getHost());
			IO.setDefaultSSLContext(SSLContext.getDefault());
	        opts = new IO.Options();
	        opts.transports = new String[]{"websocket"};
			socket = IO.socket(conf.getHost(), opts);
			addHeaders();
			System.out.println("Created websocket: "+socket);
	        socket.on(Socket.EVENT_CONNECT, new Listener() {
	            @Override
	            public void call(Object... objects) {
	            	connectionStatus.setValue(connectionStatus.getValue().connect());
					isConnected.compareAndSet(false, true);
					String[] channelsArray = channels.toArray(new String[channels.size()]);
					for (String channel : channelsArray) {
						connectToChannel(channel);
					}
					connectedFuture.resolve(null);
				}
	        });
	        socket.on(Socket.EVENT_DISCONNECT, new Listener() {
	            @Override
	            public void call(Object... objects) {
	            	connectionStatus.setValue(connectionStatus.getValue().close());
					System.out.println("Socket disconnected: "+socket);
					for (String channel : channels) {
						notifyChannelDisconnected(channel);
					}
					isConnected.compareAndSet(true, false);
	            }
	        });
	        socket.on(Socket.EVENT_ERROR, new Listener() {
	            @Override
	            public void call(Object... objects) {
	            	System.out.println(objects);
	            	//connectionStatus.setValue(connectionStatus.getValue().error(ex));
	            	/*connectionStatus.setValue(connectionStatus.getValue().error(ex));
					connectedFuture.reject(ex);
					ex.printStackTrace();
					if (connectionStatus.getValue().isAuthFailure()) {
						return; //Don't try to reconnect it will just fail again for the same reason anyway.
					}
					try {
						onDisconnect();						
						isConnected.compareAndSet(true, false);
						socket = createSocket();
						socket.connect(this);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}*/
	            }
	        });
			socket.connect();
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
				socket.emit("connectToChannel", message, new Ack() {
					@Override
					public void call(Object... args) {
						try {
							if(args.length == 1 && args[0] instanceof JSONObject){
								boolean connectedToChannel = ((JSONObject) args[0]).getBoolean("connectedToChannel");
								if(connectedToChannel){
									notifyChannelConnected(channel);
									if (sync) {
										connectedFuture.resolve(null);
									}
								}
								else {
									connectedFuture.reject(new IOException("Couldn't connect to channel "+channel));
								}
							}
							
						} catch (Exception e) {
							e.printStackTrace();
							if (sync) {
								connectedFuture.reject(e);
							}
						}
					}
				});
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
				socket.emit("disconnectFromChannel", message, new Ack() {
					
					@Override
					public void call(Object... args) {
						try {
							if(args.length == 1 && args[0] instanceof JSONObject){
								boolean disconnectedFromChannel = ((JSONObject) args[0]).getBoolean("disconnectedFromChannel");
								if(disconnectedFromChannel){
									notifyChannelDisconnected(channel);
								}
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}						
					}
				});
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
	
	@Override
	public void addMessageHandler(final IMessageHandler messageHandler) {
		super.addMessageHandler(messageHandler);
		this.socket.on(messageHandler.getMessageType(), new Listener() {
			@Override
			public void call(Object... args) {
				if(args.length == 1 && args[0] instanceof JSONObject){
					messageHandler.handle(messageHandler.getMessageType(), (JSONObject) args[0]);
                }
			}
		});
	}

	@Override
	public void removeMessageHandler(IMessageHandler messageHandler) {
		super.removeMessageHandler(messageHandler);
		socket.off(messageHandler.getMessageType());
	}

    private void addHeaders() {
        socket.io().on(Manager.EVENT_TRANSPORT, new Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport) args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, new Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        headers.put("X-flux-user-name", Collections.singletonList(conf.getUser()));
                        headers.put("X-flux-user-token", Collections.singletonList(conf.getToken()));
                    }
                });
            }
        });
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
