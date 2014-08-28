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
	private IChannelListener channelListener = new IChannelListener() {
		@Override
		public void connected(String userChannel) {
			unsetScheduledShutdown();
			unsetKeepAliveDelayedMessage();
			setKeepAliveDelayedMessage();
		}

		@Override
		public void disconnected(String userChannel) {
			unsetScheduledShutdown();
			unsetKeepAliveDelayedMessage();
			/*
			 * If no channel is connected than keep alive broadcast will
			 * definitely not get any replies and shutdown would occur
			 */
			setKeepAliveDelayedMessage();
		}
	};
	
	public KeepAliveConnector(IMessagingConnector messaingConnector, String serviceTypeId) {
		this(messaingConnector, serviceTypeId, KEEP_ALIVE_DELAY, KEEP_ALIVE_RESPONSE_WAIT_TIME);
	}
	
	public KeepAliveConnector(IMessagingConnector messaingConnector, String serviceTypeId, long keepAliveDelay, long keepAliveResponseTimeout) {
		this.mc = messaingConnector;
		this.serviceTypeId = serviceTypeId;
		this.executor = Executors.newScheduledThreadPool(2);
		this.keepAliveDelay = keepAliveDelay;
		this.keepAliveResponseTimeout = keepAliveResponseTimeout;
		this.mc.addMessageHandler(keepAliveResponseHandler);
		setKeepAliveDelayedMessage();
		mc.addChannelListener(channelListener);
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
		scheduledKeepAliveMessage = this.executor.schedule(new Runnable() {
			@Override
			public void run() {
				setScheduledShutdown();
			}
		}, keepAliveDelay, TimeUnit.SECONDS);
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
			message.put("username", mc.getChannel());
			message.put("service", serviceTypeId);
			mc.send(SERVICE_REQUIRED_REQUEST, message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void dispose() {
		mc.removeChannelListener(channelListener);
		unsetScheduledShutdown();
		unsetKeepAliveDelayedMessage();
		executor.shutdown();
		mc.removeMessageHandler(keepAliveResponseHandler);
		for (IMessageHandler messageHandler : messageHandlers) {
			mc.removeMessageHandler(messageHandler);
		}
	}

}
