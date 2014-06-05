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

import org.eclipse.flux.core.IConnectionListener;
import org.eclipse.flux.core.IMessageHandler;
import org.eclipse.flux.core.IMessagingConnector;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public abstract class AbstractMessagingConnector implements IMessagingConnector {
	
	private Collection<IConnectionListener> connectionListeners;
	private ConcurrentMap<String, Collection<IMessageHandler>> messageHandlers;
	
	public AbstractMessagingConnector() {
		this.connectionListeners = new ConcurrentLinkedDeque<>();
		this.messageHandlers = new ConcurrentHashMap<>();
	}
	
	@Override
	public void addConnectionListener(IConnectionListener connectionListener) {
		this.connectionListeners.add(connectionListener);
	}

	@Override
	public void removeConnectionListener(IConnectionListener connectionListener) {
		this.connectionListeners.remove(connectionListener);
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
	
	protected void notifyConnected() {
		for (IConnectionListener connectionListener : connectionListeners) {
			connectionListener.connected();
		}
	}
	
	protected void notifyDisconnected() {
		for (IConnectionListener connectionListener : connectionListeners) {
			connectionListener.disconnected();
		}
	}
	
	protected void handleIncomingMessage(String messageType, JSONObject message) {
		Collection<IMessageHandler> handlers = this.messageHandlers.get(messageType);
		if (handlers != null) {
			for (IMessageHandler handler : handlers) {
				if (handler.canHandle(messageType, message)) {
					handler.handleMessage(messageType, message);
				}
			}
		}
	}

}
