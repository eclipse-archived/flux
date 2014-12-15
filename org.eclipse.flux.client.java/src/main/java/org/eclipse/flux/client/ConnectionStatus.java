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

import io.socket.SocketIOException;

import org.eclipse.flux.client.util.ExceptionUtil;

/**
 * An immutable object that represent the 'status' of a Flux Messaging Connector.
 * 
 * All connection statuses are created starting from the 'INITIALIZING_STATUS' by 
 * calling methods that create new status as a transition from a previous
 * status.
 * 
 * @author Kris De Volder
 */
public class ConnectionStatus {
	
	enum Kind {
		INITIALIZING,
		CONNECTED,
		CLOSED 
	}
	
	private final Kind kind;
	private final Throwable error;
	
	public static final ConnectionStatus INITIALIZING = new ConnectionStatus(Kind.INITIALIZING, null);

	/**
	 * If a connection is closed because of an error then a Throwable may be attached to
	 * track the cause.
	 */
	private ConnectionStatus(Kind kind, Throwable error) {
		this.kind = kind;
		this.error = error;
	};
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("ConnectionStatus(");
		s.append(kind);
		if (error!=null) {
			s.append(" = ");
			s.append(ExceptionUtil.getMessage(error));
			s.append(")");
		}
		return s.toString();
	}
	
	/**
	 * TRanstition to 'connected' state. Called when a connection was succesfully
	 * established. Any prior error info is cleared at this point.
	 */
	public ConnectionStatus connect() {
		return new ConnectionStatus(Kind.CONNECTED, null);
	}

	/**
	 * Transition from current state to error state.
	 */
	public ConnectionStatus error(Throwable error) {
		return new ConnectionStatus(Kind.CLOSED, error);
	}

	/**
	 * Connector may try to reconnect after an error. It will transition into
	 * 'initializing state but retains the error so that someone examining
	 * the status may be able to determine an error has occurred (and what it was).
	 */
	public ConnectionStatus reconnect() {
		return new ConnectionStatus(Kind.INITIALIZING, error);
	}

	/**
	 * Transition to closed state.
	 */
	public ConnectionStatus close() {
		return new ConnectionStatus(Kind.CLOSED, error);
	}

	public boolean isAuthFailure() {
		if (error instanceof SocketIOException) {
			SocketIOException e = (SocketIOException) error;
			String msg = e.getMessage();
			return msg!=null && msg.contains("handshaking");
		}
		return false;
	}

	public boolean isConnected() {
		return kind==Kind.CONNECTED;
	}
	
}
