package org.eclipse.flux.service.common;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.json.JSONException;
import org.json.JSONObject;

public final class MessageConnector {
	
	static Map<URL, MessageConnector> pool = new HashMap<URL, MessageConnector>();
	
	private AtomicBoolean connected = new AtomicBoolean(false);
	private SocketIO socket;
	private ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers = new ConcurrentHashMap<String, Collection<IMessageHandler>>();
	private ConcurrentLinkedQueue<IConnectionListener> connectionListeners = new ConcurrentLinkedQueue<IConnectionListener>();
	final private String host;
	private String userChannel;
	private String login;
	private String token;
	
	public MessageConnector(final String host, final String login, String token) {
		this.host = host;
		this.login = login;
		this.token = token;
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			this.socket = createSocket(host);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connect(final String userChannel) {
		if (this.userChannel != null) {
			switchChannel(userChannel);
		} else {
			this.socket.connect(new IOCallback() {
	
				@Override
				public void on(String messageType, IOAcknowledge arg1, Object... data) {
					if (data.length == 1 && data[0] instanceof JSONObject) {
						handleIncomingMessage(messageType, (JSONObject)data[0]);
					}
				}
	
				@Override
				public void onConnect() {
					try {
						connectToChannel(userChannel);
					}
					catch (JSONException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public void onDisconnect() {
					processDisconnect();
				}
	
				@Override
				public void onError(SocketIOException ex) {
					ex.printStackTrace();
					
					try {
						disconnect();
					} catch (Throwable t) {
						t.printStackTrace();
					}
					
					try {
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
		}
	}
	
	private void switchChannel(final String userChannel) {
		try {
			JSONObject message = new JSONObject();
			message.put("channel", this.userChannel);
	
			socket.emit("disconnectFromChannel", new IOAcknowledge() {
				@Override
				public void ack(Object... answer) {
					try {
						if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("disconnectedFromChannel")) {
							processDisconnect();
							if (userChannel != null) {
								connectToChannel(userChannel);
							}
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

	private void connectToChannel(final String userChannel) throws JSONException {
		JSONObject message = new JSONObject();
		message.put("channel", userChannel);
		socket.emit("connectToChannel", new IOAcknowledge() {

			public void ack(Object... answer) {
				try {
					if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("connectedToChannel")) {
						processConnect(userChannel);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}, message);
	}

	private SocketIO createSocket(String host) throws MalformedURLException {
		SocketIO socket = new SocketIO(host);
		socket.addHeader("X-flux-user-name", login);
		socket.addHeader("X-flux-user-token", token);
		return socket;
	}
	
	protected void handleIncomingMessage(String messageType, JSONObject message) {
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

	public boolean isConnected() {
		return connected.get();
	}
	
	public void addMessageHandler(IMessageHandler messageHandler) {
		this.messageHandlers.putIfAbsent(messageHandler.getMessageType(), new ConcurrentLinkedDeque<IMessageHandler>());
		this.messageHandlers.get(messageHandler.getMessageType()).add(messageHandler);
	}

	public void removeMessageHandler(IMessageHandler messageHandler) {
		this.messageHandlers.get(messageHandler.getMessageType()).remove(messageHandler);
	}
	
	public void addConnectionListener(IConnectionListener listener) {
		this.connectionListeners.add(listener);
	}
	
	public void removeConnectionListener(IConnectionListener listener) {
		this.connectionListeners.remove(listener);
	}
	
	private void notifyConnected(String userChannel) {
		for (IConnectionListener listener : connectionListeners) {
			try {
				listener.connected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private void notifyDisconnected(String userChannel) {
		for (IConnectionListener listener : connectionListeners) {
			try {
				listener.disconnected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void disconnect() {
		if (socket != null) {
			socket.disconnect();
		}
	}
	
	public String getHost() {
		return host;
	}
	
	private void processConnect(String userChannel) {
		this.connected.compareAndSet(false, true);
		if (userChannel != null) {
			this.userChannel = userChannel;
			notifyConnected(userChannel);
		}
	}
	
	private void processDisconnect() {
		this.connected.compareAndSet(true, false);
		if (this.userChannel != null) {
			String userChannel = this.userChannel;
			this.userChannel = null;
			notifyDisconnected(userChannel);
		}
	}
	
 	
}
