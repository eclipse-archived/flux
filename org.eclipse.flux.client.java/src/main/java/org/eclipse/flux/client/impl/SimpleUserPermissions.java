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

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.config.UserPermissions;
import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.util.Assert;
import org.eclipse.flux.client.util.Console;

/**
 * Simple implementation of UserPermissions that allows the super user to do anything
 * and any other user to connect only to their own channel. 
 * 
 * @author Kris De Volder
 */
public class SimpleUserPermissions implements UserPermissions {
	
	/**
	 * UserID of the Flux user that initiated the flux connection.
	 */
	final private String user;
	
	public SimpleUserPermissions(FluxConfig conf) {
		Assert.assertTrue(conf.getUser()!=null);
		this.user = conf.getUser();
	}

	@Override
	public String getUser() {
		return user;
	}
	
	/**
	 * Check whether a the connection owner socket has the authorization needed to
	 * join a channel. Will throw some exception if the check failed and return normally
	 * otherwise.
	 */
	public void checkChannelJoin(String channel) throws UserPermissionException {
		Assert.assertTrue(channel!=null);
		if (user.equals(MessageConstants.SUPER_USER) || user.equals(channel)) {
			console.log("ACCEPT '"+user+"' to join '"+channel+"'");
			return;
		} else {
			console.log("REJECT '"+user+"' to join '"+channel+"'");
			throw error("'"+user+"' is not allowed to join channel '"+channel+"'");
		}
	}

	///////////////////////////////////////////////////////////////////
	
	public static class UserPermissionException extends Exception {
		private static final long serialVersionUID = 1L;
		public UserPermissionException(String msg) {
			super(msg);
		}
	}

	private static final Console console = Console.get(SimpleUserPermissions.class.getName());
	
	private UserPermissionException error(String msg) {
		return new UserPermissionException(msg);
	}

}
