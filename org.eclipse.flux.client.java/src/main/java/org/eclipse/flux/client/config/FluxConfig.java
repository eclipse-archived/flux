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
package org.eclipse.flux.client.config;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;

/**
 * FluxConfig contains information needed to create and configure 
 * connection to flux bus.
 * 
 * TODO: something about this interface feels iffy. To put a finger on it...
 * in cf-deployer application we need to pass this kind of info around 
 * around, but without the value of 'user' being set.
 * I.e. the app will deal with multiple users (as these users connect to the app)
 * and needs something to be able to create fluxconfig objects for each user.
 * 
 * @author Kris De Volder
 */
public interface FluxConfig {

	MessageConnector connect(FluxClient fluxClient) throws Exception;
	
	/**
	 * Every flux MessageConnector is 'owned' by a specific Flux user. This method
	 * returns the Flux user id associated with the flux connections that will be 
	 * created from this config.
	 */
	String getUser();

	/**
	 * Convert this config into a equivalent SocketIO config, thus allowing
	 * a client running in an environment that does not have direct access
	 * to RabbitMQ to connect using a websocket-based implementation.
	 */
	SocketIOFluxConfig toSocketIO();
	
	/**
	 * An UserPermissions instance provides various methods to verify whether
	 * a flux user is allowed to do certain things.
	 */
	UserPermissions permissions();
	
}
