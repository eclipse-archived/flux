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

/**
 * UserPermissions instance provides various methods that check whether the associated
 * Flux user is allowed to do something.
 * <p>
 * This instance is not responsible for authentication (i.e. user identity is not
 * verified, it is assumed this is done by an instance of 'Authenticator'.
 */
public interface UserPermissions {

	/**
	 * @return The user for which this this instance verifies permissions.
	 */
	String getUser();

	/**
	 * Check whether the connection 'owner' has the authorization needed to
	 * join a channel. Will throw some exception if the check failed and return normally
	 * otherwise.
	 */
	void checkChannelJoin(String channelName) throws Exception;

}
