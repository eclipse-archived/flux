package org.eclipse.flux.service.common;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLContext;

import org.json.JSONException;
import org.json.JSONObject;

public final class MessageConnector {
	
	static Map<URL, MessageConnector> pool = new HashMap<URL, MessageConnector>();
	
	private SocketIO socket;
	private ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers = new ConcurrentHashMap<String, Collection<IMessageHandler>>();
	private ConcurrentLinkedQueue<IConnectionListener> connectionListeners = new ConcurrentLinkedQueue<IConnectionListener>();
	final private String host;
	private String login;
	private String token;
	private Set<String> channels = Collections.synchronizedSet(new HashSet<String>());
	
	public MessageConnector(final String host, final String login, String token) {
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
				}
	
				@Override
				public void onDisconnect() {
					while (!channels.isEmpty()) {
						String channel = channels.iterator().next();
						channels.remove(channel);
						notifyDisconnected(channel);
					}
				}
	
				@Override
				public void onError(SocketIOException ex) {
					ex.printStackTrace();					
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connectToChannel(final String channel) {
		if (channel != null && !channels.contains(channel)) {
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
								notifyConnected(channel);
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
		if (removed) {
			try {
				JSONObject message = new JSONObject();
				message.put("channel", channel);
				socket.emit("disconnectFromChannel", new IOAcknowledge() {
	
					public void ack(Object... answer) {
						try {
							if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("disconnectedFromChannel")) {
								notifyDisconnected(channel);
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
		socket.addHeader("X-flux-user-name", login);
		socket.addHeader("X-flux-user-token", token);
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
		return channels.contains(channel);
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
		socket.disconnect();
	}
	
	public String getHost() {
		return host;
	}
	
}
