package org.eclipse.flux.service.common;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.json.JSONException;
import org.json.JSONObject;

public final class MessageConnector {
	
	private static final String SUPER_USER = "$super$";
	
	private AtomicBoolean connected = new AtomicBoolean(false);
	private SocketIO socket;
	private ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers = new ConcurrentHashMap<String, Collection<IMessageHandler>>();
	private List<IConnectionListener> connectionListeners = new ArrayList<IConnectionListener>();
	
	public MessageConnector(final String host) {
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			this.socket = new SocketIO(host);
			this.socket.connect(new IOCallback() {
	
				@Override
				public void on(String messageType, IOAcknowledge arg1, Object... data) {
					if ("getLiveResourcesResponse".equals(messageType)) {
						System.out.println(data[0]);
					}
					if (data.length == 1 && data[0] instanceof JSONObject) {
						handleIncomingMessage(messageType, (JSONObject)data[0]);
					}
				}
	
				@Override
				public void onConnect() {
					try {
						JSONObject message = new JSONObject();
						message.put("channel", SUPER_USER);
						socket.emit("connectToChannel", new IOAcknowledge() {
	
							public void ack(Object... answer) {
								try {
									if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("connectedToChannel")) {
										if (connected.compareAndSet(false, true)) {
											notifyConnected();
										}
									}
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
							
						}, message);
					}
					catch (JSONException e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public void onDisconnect() {
					connected.compareAndSet(true, false);
					notifyDisconnected();
				}
	
				@Override
				public void onError(SocketIOException ex) {
					ex.printStackTrace();
					
					try {
						socket = new SocketIO(host);
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
	
	protected void handleIncomingMessage(String messageType, JSONObject message) {
		Collection<IMessageHandler> handlers = this.messageHandlers.get(messageType);
		if (handlers != null) {
			for (IMessageHandler handler : handlers) {
				if (handler.canHandle(messageType, message)) {
					handler.handle(messageType, message);
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
	
	public void dispose() {
		socket.disconnect();
	}
	
	public void addConnectionListener(IConnectionListener listener) {
		this.connectionListeners.add(listener);
	}
	
	public void removeConnectionListener(IConnectionListener listener) {
		this.connectionListeners.remove(listener);
	}
	
	private void notifyConnected() {
		for (IConnectionListener listener : connectionListeners) {
			listener.connected();
		}
	}
	
	private void notifyDisconnected() {
		for (IConnectionListener listener : connectionListeners) {
			listener.disconnected();
		}
	}
}
