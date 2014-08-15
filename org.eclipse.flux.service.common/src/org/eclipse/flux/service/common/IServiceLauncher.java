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
package org.eclipse.flux.service.common;

/**
 * Starts and stops IDE Tooling Service
 * 
 * @author aboyko
 *
 */
public interface IServiceLauncher {
	
	void init();
	
	boolean isInitializationFinished();
	
	/**
	 * Starts IDE Tooling Service
	 * 
	 * @param user the user for which to start the service
	 * @return <code>true</code> if service has been started successfully
	 */
	boolean startService(String user);
	
	/**
	 * Stops IDE Tooling Service
	 * 
	 * @param user the user for which to stop the service
	 * @return <code>true</code> if service has been stopped successfully
	 */
	boolean stopService(String user);
	
	void dispose();
	
}
