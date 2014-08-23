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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

public final class KeepAliveConnector {
	
	private static final String SERVICE_REQUIRED_REQUEST = "serviceRequiredRequest";
	private static final String SERVICE_REQUIRED_RESPONSE = "serviceRequiredResponse";

	private static final long KEEP_ALIVE_DELAY = 60; // 60 seconds
	private static final long KEEP_ALIVE_RESPONSE_WAIT_TIME = 5; // 5 seconds
	
	private IMessagingConnector mc;
	private String serviceTypeId;
	private String user;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> scheduledKeepAliveMessage = null;
	private ScheduledFuture<?> scheduledShutDown = null;
	private long keepAliveDelay;
	private long keepAliveResponseTimeout;
	private List<IMessageHandler> messageHandlers = Collections.emptyList();
	private IMessageHandler keepAliveResponseHandler = new AbstractMessageHandler(SERVICE_REQUIRED_RESPONSE) {
		@Override
		public void handleMessage(String messageType, JSONObject message) {
			unsetScheduledShutdown();
			setKeepAliveDelayedMessage();
		}
	};
	
	public KeepAliveConnector(IMessagingConnector messaingConnector, String user, String serviceTypeId) {
		this(messaingConnector, user, serviceTypeId, KEEP_ALIVE_DELAY, KEEP_ALIVE_RESPONSE_WAIT_TIME);
	}
	
	public KeepAliveConnector(IMessagingConnector messaingConnector, String user, String serviceTypeId, long keepAliveDelay, long keepAliveResponseTimeout) {
		this.mc = messaingConnector;
		this.serviceTypeId = serviceTypeId;
		this.user = user;
		this.executor = Executors.newScheduledThreadPool(2);
		this.keepAliveDelay = keepAliveDelay;
		this.keepAliveResponseTimeout = keepAliveResponseTimeout;
		this.mc.addMessageHandler(keepAliveResponseHandler);
		setKeepAliveDelayedMessage();
	}
	
	private synchronized void unsetKeepAliveDelayedMessage() {
		if (scheduledKeepAliveMessage != null && !scheduledKeepAliveMessage.isCancelled()) {
			scheduledKeepAliveMessage.cancel(false);
		}
	}
	
	private synchronized void unsetScheduledShutdown() {
		if (scheduledShutDown != null && !scheduledShutDown.isCancelled()) {
			scheduledShutDown.cancel(false);
		}
	}
	
	private synchronized void setKeepAliveDelayedMessage() {
		if (user != null) {
			scheduledKeepAliveMessage = this.executor.schedule(new Runnable() {
				@Override
				public void run() {
					setScheduledShutdown();
				}
			},
			keepAliveDelay, TimeUnit.SECONDS);
		}
	}
	
	private synchronized void setScheduledShutdown() {
		try {
			scheduledShutDown = executor.schedule(new Runnable() {
				@Override
				public void run() {
					/*
					 * Clean up needs to be done right before disconnecting from
					 * channel would work here
					 */
					//mc.connectChannel(Constants.SUPER_USER);
					System.exit(0);
				}
			}, keepAliveResponseTimeout, TimeUnit.SECONDS);
			JSONObject message = new JSONObject();
			message.put("username", user);
			message.put("service", KeepAliveConnector.this.serviceTypeId);
			mc.send(SERVICE_REQUIRED_REQUEST, message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
//	public void setKeepAliveMessageTypes(String[] messageTypes) {
//		unsetScheduledShutdown();
//		unsetKeepAliveDelayedMessage();
//		for (IMessageHandler messageHandler : messageHandlers) {
//			mc.removeMessageHandler(messageHandler);
//		}
//		messageHandlers = new ArrayList<IMessageHandler>(messageTypes.length);
//		for (String messageType : messageTypes) {
//			messageHandlers.add(new AbstractMessageHandler(messageType) {				
//				@Override
//				public void handleMessage(String messageType, JSONObject message) {
//					try {
//						if (message.has("username") && message.getString("username").equals(KeepAliveConnector.this.user)) {
//							resetEverything();
//						}
//					} catch (JSONException e) {
//						e.printStackTrace();
//					}
//				}
//			});
//		}
//		for (IMessageHandler messageHandler : messageHandlers) {
//			mc.addMessageHandler(messageHandler);
//		}
//		setKeepAliveDelayedMessage();
//	}
//	
//	private synchronized void resetEverything() {
//		unsetScheduledShutdown();
//		unsetKeepAliveDelayedMessage();
//		setKeepAliveDelayedMessage();
//	}
	
	public synchronized void setUser(String user) {
		unsetScheduledShutdown();
		unsetKeepAliveDelayedMessage();
		this.user = user;
		setKeepAliveDelayedMessage();
	}
	
	public void dispose() {
		unsetScheduledShutdown();
		unsetKeepAliveDelayedMessage();
		executor.shutdown();
		mc.removeMessageHandler(keepAliveResponseHandler);
		for (IMessageHandler messageHandler : messageHandlers) {
			mc.removeMessageHandler(messageHandler);
		}
	}

}
