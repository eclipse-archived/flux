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
package org.springframework.social.showcase.cloudfoundry;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

public class CloudFoundryManagerImpl implements CloudFoundryManager {

	private Map<String, CloudFoundry> map = new HashMap<>();

	@Override
	public synchronized CloudFoundry getConnection(Principal currentUser) {
		return map.get(currentUser.getName());
	}

	@Override
	public synchronized void putConnection(Principal currentUser, CloudFoundry cloudFoundry) {
		map.put(currentUser.getName(), cloudFoundry);
	}

}
