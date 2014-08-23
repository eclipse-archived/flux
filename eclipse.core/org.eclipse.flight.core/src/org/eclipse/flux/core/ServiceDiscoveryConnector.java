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
package org.eclipse.flux.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Minimal implementation of a Service Connector that is suitable to use in a running instance of
 * Eclipse that a user has started manually and is simply using as their development IDE.
 * <p>
 * This allows other flux clients to discover this service as an available JDT service that
 * is already assigned to the user that is running the eclipse instance.
 */
public class ServiceDiscoveryConnector {

	private static final String DISCOVER_SERVICE_REQUEST = "discoverServiceRequest";
	private static final String DISCOVER_SERVICE_RESPONSE = "discoverServiceResponse";
	private static final String SERVICE_STATUS_CHANGE = "serviceStatusChange";
	private static final String START_SERVICE_REQUEST = "startServiceRequest";
	private static final String START_SERVICE_RESPONSE = "startServiceResponse";
	
	protected static final String[] COPY_PROPS = {
		"service",
		"requestSenderID",
		"username",
		"callback_id"
	};
	private String user;
	private IMessagingConnector mc;
	private String serviceTypeId;
	
	private List<Runnable> onDispose = new ArrayList<Runnable>();
	
	private boolean forMe(JSONObject message) {
		try {
			return message.getString("service").equals(serviceTypeId);
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public ServiceDiscoveryConnector(IMessagingConnector messagingConnector, String channel, String serviceTypeId, boolean keepAlive) {
		this.user = channel;
		this.mc = messagingConnector;
		this.serviceTypeId = serviceTypeId;

		sendStatus("ready");
		
		handler(new AbstractMessageHandler(DISCOVER_SERVICE_REQUEST) {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				try {
					if (forMe(message) && (message.get("username").equals(user) || Constants.SUPER_USER.equals(user))) {
						JSONObject response = new JSONObject(message, COPY_PROPS);
						response.put("status", message.get("username").equals(user) ? "ready" : "available");
						mc.send(DISCOVER_SERVICE_RESPONSE, response);
					}
				} catch (Exception e) {
					throw new Error(e);
				}
			}
		});
		
		this.mc.addMessageHandler(new AbstractMessageHandler(START_SERVICE_REQUEST) {

			@Override
			public void handleMessage(String messageType, JSONObject message) {
				mc.removeMessageHandler(this);
				try {
					user = message.getString("username");
					JSONObject serviceStartedMessage = new JSONObject(message, COPY_PROPS);
					mc.send(START_SERVICE_RESPONSE, serviceStartedMessage);
					sendStatus("starting");
					mc.connectChannel(user);
					sendStatus("ready");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					user = null;
				}
				
				
			}
			
		});
		
	}

	private synchronized void handler(final IMessageHandler h) {
		onDispose.add(new Runnable() {
			@Override
			public void run() {
				mc.removeMessageHandler(h);
			}
		});
		mc.addMessageHandler(h);
	}

	public synchronized void dispose() {
		try {
			if (mc!=null) {
				sendStatus("unavailable", "Shutdown");
				for (Runnable r : onDispose) {
					r.run();
				}
			}
			mc = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendStatus(String status) {
		sendStatus(status, null);
	}

	private void sendStatus(String status, String error) {
		try {
			JSONObject msg = new JSONObject();
			msg.put("username", user);
			msg.put("service", serviceTypeId);
			msg.put("status", status);
			if (error!=null) {
				msg.put("error", error);
			}
			mc.send(SERVICE_STATUS_CHANGE, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
