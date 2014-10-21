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

import org.eclipse.flux.client.impl.SimpleUserPermissions;

public abstract class AbstractFluxConfig implements FluxConfig {

	private String user;
	private UserPermissions permissions;
	
	public AbstractFluxConfig(String user) {
		this.user = user;
		this.permissions = createUserPermissions();
	}
	
	protected UserPermissions createUserPermissions() {
		return new SimpleUserPermissions(this);
	}

	@Override
	public String getUser() {
		return user;
	}
	
	@Override
	public String toString() {
		return getClass().getName()+"("+user+")";
	}
	
	@Override
	public UserPermissions permissions() {
		return permissions;
	}
	
}
