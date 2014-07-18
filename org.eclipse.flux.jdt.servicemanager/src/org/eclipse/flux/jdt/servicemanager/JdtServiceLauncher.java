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
package org.eclipse.flux.jdt.servicemanager;

import org.eclipse.flux.service.common.IServiceLauncher;

/**
 * JDT Service start and stop logic.
 * 
 * @author aboyko
 *
 */
public class JdtServiceLauncher implements IServiceLauncher {

	@Override
	public boolean startService(String user) {
		System.out.println("Starting JDT for " + user);
		return true;
	}

	@Override
	public boolean stopService(String user) {
		System.out.println("Stopping JDT for " + user);
		return true;
	}

}
