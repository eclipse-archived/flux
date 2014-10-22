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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.eclipse.flux.client.IChannelListener;
import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.json.JSONObject;

public abstract class AbstractMessageConnector implements MessageConnector {

	private final ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers = new ConcurrentHashMap<String, Collection<IMessageHandler>>();
	protected final ExecutorService executor;
	private ConcurrentLinkedQueue<IChannelListener> channelListeners = new ConcurrentLinkedQueue<IChannelListener>();
	
	public AbstractMessageConnector(ExecutorService executor) {
		this.executor = executor;
	}

	protected void handleIncomingMessage(final String messageType, final JSONObject message) {
		Collection<IMessageHandler> handlers = this.messageHandlers.get(messageType);
		if (handlers != null) {
			for (final IMessageHandler handler : handlers) {
				try {
					if (handler.canHandle(messageType, message)) {
						executor.execute(new Runnable() {
							public void run() {
								handler.handle(messageType, message);
							}
						});
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	public void addChannelListener(IChannelListener listener) {
		this.channelListeners.add(listener);
	}
	
	public void removeChannelListener(IChannelListener listener) {
		this.channelListeners.remove(listener);
	}
	
	protected void notifyChannelConnected(String userChannel) {
		for (IChannelListener listener : channelListeners) {
			try {
				listener.connected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	protected void notifyChannelDisconnected(String userChannel) {
		for (IChannelListener listener : channelListeners) {
			try {
				listener.disconnected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public void addMessageHandler(IMessageHandler messageHandler) {
		this.messageHandlers.putIfAbsent(messageHandler.getMessageType(), new ConcurrentLinkedDeque<IMessageHandler>());
		this.messageHandlers.get(messageHandler.getMessageType()).add(messageHandler);
	}

	public void removeMessageHandler(IMessageHandler messageHandler) {
		Collection<IMessageHandler> handlers = this.messageHandlers.get(messageHandler.getMessageType());
		if (handlers != null) {
			handlers.remove(messageHandler);
		}
	}
}
