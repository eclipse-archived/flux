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
package org.eclipse.flux.client.java;

import junit.framework.TestCase;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.client.config.UserPermissions;
import org.eclipse.flux.client.config.FluxConfig;
import org.eclipse.flux.client.config.SocketIOFluxConfig;
import org.eclipse.flux.client.impl.SimpleUserPermissions;

public class SimpleUserPermissionsTest extends TestCase {
	
	FluxConfig conf(final String user) {
		//Mock Flux config 
		return new FluxConfig() {
			@Override
			public SocketIOFluxConfig toSocketIO() {
				return null;
			}
			
			@Override
			public String getUser() {
				return user;
			}
			
			@Override
			public MessageConnector connect(FluxClient fluxClient) throws Exception {
				throw new Error("Not impelemented");
			}

			@Override
			public UserPermissions permissions() {
				return new SimpleUserPermissions(this);
			}
		};
	}
	
	public void testSuperCanJoinSuper() throws Exception {
		String user = MessageConstants.SUPER_USER;
		String channel = MessageConstants.SUPER_USER;
		join(user, channel);
	}
	
	public void testUserCanJoinSelf() throws Exception {
		String user = "testUser";
		String channel = "testUser";
		join(user, channel);
	}

	public void testUserCanNotJoinSuper() throws Exception {
		String user = "testUser";
		String channel = MessageConstants.SUPER_USER;
		joinShouldFail(user, channel);
	}
	
	public void testUserCannotJoinOtherUser() throws Exception {
		joinShouldFail("bob", "john");
	}

	private void joinShouldFail(String user, String channel) {
		try {
			join(user, channel);
			fail("Should have rejected '"+user+"' to join '"+channel);
		} catch (Exception e) {
			assertEquals("'"+user+"' is not allowed to join channel '"+channel+"'", e.getMessage());
		}
	}

	private void join(String user, String channel) throws Exception {
		FluxConfig conf = conf(user);
		UserPermissions auth = conf.permissions();
		auth.checkChannelJoin(channel);
	}

}
