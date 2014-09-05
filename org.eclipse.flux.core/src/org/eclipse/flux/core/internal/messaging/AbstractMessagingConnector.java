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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.flux.core.IChannelListener;
import org.eclipse.flux.core.IConnectionListener;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public abstract class AbstractMessagingConnector implements IMessagingConnector {
	
	private Collection<IChannelListener> channelListeners;
	private Collection<IConnectionListener> connectionListeners;
	private ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers;
	
	public AbstractMessagingConnector() {
		this.connectionListeners = new ConcurrentLinkedDeque<IConnectionListener>();
		this.channelListeners = new ConcurrentLinkedDeque<IChannelListener>();
		this.messageHandlers = new ConcurrentHashMap<>();
	}
	
	@Override
	public void addChannelListener(IChannelListener connectionListener) {
		this.channelListeners.add(connectionListener);
	}

	@Override
	public void removeChannelListener(IChannelListener connectionListener) {
		this.channelListeners.remove(connectionListener);
	}
	
	@Override
	public void addMessageHandler(IMessageHandler messageHandler) {
		this.messageHandlers.putIfAbsent(messageHandler.getMessageType(), new ConcurrentLinkedDeque<IMessageHandler>());
		this.messageHandlers.get(messageHandler.getMessageType()).add(messageHandler);
	}

	@Override
	public void removeMessageHandler(IMessageHandler messageHandler) {
		this.messageHandlers.get(messageHandler.getMessageType()).remove(messageHandler);
	}
	
	protected void notifyChannelConnected(String userChannel) {
		for (IChannelListener channelListener : channelListeners) {
			try {
				channelListener.connected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	protected void notifyChannelDisconnected(String userChannel) {
		for (IChannelListener channelListener : channelListeners) {
			try {
				channelListener.disconnected(userChannel);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	protected void notifyConnected() {
		for (IConnectionListener connectionListener : connectionListeners) {
			try {
				connectionListener.connected();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	protected void notifyDisconnected() {
		for (IConnectionListener connectionListener : connectionListeners) {
			try {
				connectionListener.disconnected();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	@Override
	public void addConnectionListener(IConnectionListener connectionListener) {
		connectionListeners.add(connectionListener);
	}

	@Override
	public void removeConnectionListener(IConnectionListener connectionListener) {
		connectionListeners.remove(connectionListener);
	}

	protected void handleIncomingMessage(final String messageType, final JSONObject message) {
		Collection<IMessageHandler> handlers = AbstractMessagingConnector.this.messageHandlers.get(messageType);
		if (handlers != null) {
			for (IMessageHandler handler : handlers) {
				if (handler.canHandle(messageType, message)) {
					handler.handleMessage(messageType, message);
				}
			}
		}
	}
	
}
