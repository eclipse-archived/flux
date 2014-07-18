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

import org.eclipse.flux.service.common.ToolingServiceManager;

/**
 * Starts and Stops JDT Flux service per user. JDT Tooling resources are considered to be all *.java and *.class resources.
 * 
 * @author aboyko
 *
 */
public class JdtServiceManager {

	private static final int CLEANUP_JDT_SERVICES_CALLBACK = "Cleanup-JDT-Services".hashCode();
	
	private static final String JDT_RESOURCE_REGEX = ".*\\.java|.*\\.class";

	/**
	 * Launches the application. If command line arguments are present, the
	 * first one is considered to be the Flux server URL.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		String host = "http://localhost:3000"; // default Flux server URL
		if (args.length > 0) {
			host = args[0];
		}
		
		ToolingServiceManager jdtServiceManager = new ToolingServiceManager(
				host, new JdtServiceLauncher()).cleanupCallbackId(
				CLEANUP_JDT_SERVICES_CALLBACK).fileFilters(
						JDT_RESOURCE_REGEX);
		
		jdtServiceManager.start();
	}

}
