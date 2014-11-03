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
package org.eclipse.flux.client;

import org.eclipse.flux.client.config.FluxConfig;
import org.json.JSONObject;

/**
 * An instance of this interface represents a connection to flux message bus.
 * <p>
 * Clients may send messages by calling the 'send' method 
 * and receive messages by adding message handlers. 
 * <p>
 * A connection has the concept of 'channels' which corresponds to a 'user'
 * more or less. Messages in flux are generally intended for, or pertaining to
 * a specific user. By connecting to a user's channel the client becomes elegible
 * to send messages on behalf of this user and receive messages on behalf of the user.
 * <p>
 * Initially when a connection is created it is not connected to any user channel and
 * will only be eligible to send/receive messages that are sent explicitly to all users.
 * <p>
 * Depending on how a IMessageConnector was created, it may have certain priviliges 
 * and limitations. E.g. a typical user may not join any channels but their own 
 * and may only send or receive messages that belong to them, as indicated by
 * the 'username' property in the message.
 * <p>
 * If a client tries doing something it is not allowed to do, then an exception 
 * may be thrown, or the operation can just silently fail.
 */
public interface MessageConnector {
	
	/**
	 * Deprecated, please use connectToChannel('myChannel') to
	 * connect to channel synchronously and avoid common bugs of the
	 * type 'oops I sent messages before the channel was connected'.
	 * <p>
	 * Also consider catching/handling exceptions connectToChannelSync('myChannel') might throw
	 * if it fails to connect to the channel.
	 */
	@Deprecated
	public void connectToChannel(final String channel);
	public void connectToChannelSync(String username) throws Exception;
	
	public void disconnectFromChannel(final String channel) throws Exception;
	void disconnectFromChannelSync(String channelName) throws Exception;
	
	public boolean isConnected(String channel);
	
	public void send(String messageType, JSONObject message) throws Exception;
	
	public void addMessageHandler(IMessageHandler messageHandler);
	public void removeMessageHandler(IMessageHandler messageHandler);
	
	public void addChannelListener(IChannelListener listener);
	public void removeChannelListener(IChannelListener listener);
	
	public void disconnect();
	
	public boolean isConnected();
	
	public FluxConfig getConfig();


}
