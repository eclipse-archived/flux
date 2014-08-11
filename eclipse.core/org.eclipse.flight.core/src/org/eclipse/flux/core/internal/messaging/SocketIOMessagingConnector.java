/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.core.internal.messaging;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;

import javax.net.ssl.SSLContext;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.flux.core.Activator;
import org.eclipse.flux.core.IMessagingConnector;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class SocketIOMessagingConnector extends AbstractMessagingConnector implements IMessagingConnector {

	protected static final long INITIAL_DELAY = 0;

	static {
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
				return true;
			}
		});
	}

	private SocketIO socket;
	private String host;	
	private transient boolean connectedToUserspace;
	private transient boolean connected;
	private String userChannel;
	private String login;
	private String token;
		
	public SocketIOMessagingConnector(String host, final String login, final String token) {
		this.host = host;
		this.login = login;
		this.token = token;
		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			socket = createSocket();
			socket.connect(new IOCallback() {
				
				private long delay = INITIAL_DELAY;
	
				@Override
				public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
				}
	
				@Override
				public void onMessage(String arg0, IOAcknowledge arg1) {
				}
	
				@Override
				public void onError(SocketIOException ex) {
					final IOCallback self = this;
					Activator.log(ex);
					new Job("Reconnect web-socket") {
						@Override
						protected IStatus run(IProgressMonitor arg0) {
							try {
								socket = createSocket();
								socket.connect(self);
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
							return Status.OK_STATUS;
						}
					}.schedule(reconnectDelay());
				}
	
				private long reconnectDelay() {
					long r = this.delay;
					this.delay = (long)((this.delay+1000)*1.1);
					return r;
				}
	
				@Override
				public void onConnect() {
					try {
						connected = true;
						delay = INITIAL_DELAY;
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
	
				@Override
				public void onDisconnect() {
					processDisconnect();
					connected = false;
				}
	
				@Override
				public void on(String event, IOAcknowledge ack, Object... data) {
					if (data.length == 1 && data[0] instanceof JSONObject) {
						handleIncomingMessage(event, (JSONObject)data[0]);
					}
				}
	
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connect(final String userChannel) {
		if (this.userChannel == userChannel || (this.userChannel != null && this.userChannel.equals(userChannel))) {
			return;
		}
		if (this.userChannel != null) {
			switchChannel(userChannel);
		} else {
			try {
				connectToChannel(userChannel);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void processConnect(String userChannel) {
		this.connectedToUserspace = true;
		if (userChannel != null) {
			this.userChannel = userChannel;
			notifyChannelConnected(userChannel);
		}
	}
	
	private void processDisconnect() {
		this.connectedToUserspace = false;
		if (this.userChannel != null) {
			String userChannel = this.userChannel;
			this.userChannel = null;
			notifyChannelDisconnected(userChannel);
		}
	}
	
	private void connectToChannel(final String userChannel) throws JSONException {
		JSONObject message = new JSONObject();
		message.put("channel", userChannel);

		socket.emit("connectToChannel", new IOAcknowledge() {
			@Override
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

	private SocketIO createSocket() throws MalformedURLException {
		SocketIO socket = new SocketIO(host);
		if (token!=null) {
			socket.addHeader("X-flux-user-name", login);
			socket.addHeader("X-flux-user-token", token);
		}
		return socket;
	}

	@Override
	public void send(String messageType, JSONObject message) {
		socket.emit(messageType, message);
	}

	@Override
	public boolean isConnected() {
		return connected && connectedToUserspace;
	}
	
	public String getLogin() {
		return login;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getHost() {
		return host;
	}

	@Override
	public void disconnect() {
		if (socket != null) {
			socket.disconnect();
		}
	}
	
	private void switchChannel(final String userChannel) {
		try {
			JSONObject message = new JSONObject();
			message.put("channel", this.userChannel);
			
			/*
			 * The call nullifies this.userChannel. So call it after creating the disconnect message
			 * Send dosconnection event to listeners before disconnecting
			 */
			processDisconnect();
	
			socket.emit("disconnectFromChannel", new IOAcknowledge() {
				@Override
				public void ack(Object... answer) {
					try {
						if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("disconnectedFromChannel")) {
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

}
