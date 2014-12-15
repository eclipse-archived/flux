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

import org.eclipse.flux.client.MessageConnector;

/**
 * Wrapper around a {@link MessageConnector} which ensures we are only connected to
 * a single channel at a time. Provides a method to switch channels.
 * 
 * @author Kris De Volder
 */
public class ChannelSwitcher {
	
	private final MessageConnector messageConnector;
	private String channelName;

	public ChannelSwitcher(MessageConnector wrappee) {
		this.messageConnector = wrappee;
	}

	public synchronized String getChannel() {
		return channelName;
	}

	public synchronized void switchToChannel(String channelName) throws Exception {
		if (channelName!=null) {
			messageConnector.disconnectFromChannelSync(channelName);
		}
		messageConnector.connectToChannelSync(channelName);
		this.channelName = channelName;
	}
	
}
