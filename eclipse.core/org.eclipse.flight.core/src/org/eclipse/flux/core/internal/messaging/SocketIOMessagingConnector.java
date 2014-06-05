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

import org.eclipse.flux.core.IMessagingConnector;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class SocketIOMessagingConnector extends AbstractMessagingConnector implements IMessagingConnector {

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
	
	public SocketIOMessagingConnector(final String username) {
		host = System.getProperty("flux-host", "http://localhost:3000");

		try {
			SocketIO.setDefaultSSLSocketFactory(SSLContext.getInstance("Default"));
			socket = new SocketIO(host);
			socket.connect(new IOCallback() {

				@Override
				public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
				}

				@Override
				public void onMessage(String arg0, IOAcknowledge arg1) {
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
				public void onConnect() {
					try {
						connected = true;
						
						JSONObject message = new JSONObject();
						message.put("channel", username);

						socket.emit("connectToChannel", new IOAcknowledge() {
							@Override
							public void ack(Object... answer) {
								try {
									if (answer.length == 1 && answer[0] instanceof JSONObject && ((JSONObject)answer[0]).getBoolean("connectedToChannel")) {
										connectedToUserspace = true;
										notifyConnected();
									}
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}
						}, message);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onDisconnect() {
					connected = false;
					notifyDisconnected();
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

	@Override
	public void send(String messageType, JSONObject message) {
		socket.emit(messageType, message);
	}

	@Override
	public boolean isConnected() {
		return connected && connectedToUserspace;
	}
	
}
