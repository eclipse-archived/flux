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
package org.eclipse.flux.cloudfoundry.deployment.service;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.config.FluxConfig;

public class CfDeploymentServiceMain {

	/**
	 * Launches the application. If command line arguments are present, the
	 * first one is considered to be the Flux server URL.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		CfDeploymentService instance = new CfDeploymentService(FluxClient.DEFAULT_INSTANCE, FluxConfig.defaultConfig());
		instance.start();
	}
	
}
