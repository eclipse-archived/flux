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

import java.io.File;
import java.io.IOException;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.Staging;

/**
 * Defines API for Cloud Foundry operations, like pushing an application. A
 * valid {@link CloudFoundryClient} is required.
 *
 */
public class CloudFoundryClientDelegate {

	private final CloudFoundryClient client;

	public CloudFoundryClientDelegate(CloudFoundryClient client) {
		this.client = client;
	}

	public void push(String appName, File location) throws IOException {

		CloudFoundryApplication localApp = new CloudFoundryApplication(appName,
				location, client);

		String deploymentName = localApp.getName();
		// Check whether it exists. if so, stop it first, otherwise create it
		CloudApplication app = client.getApplication(deploymentName);
		if (app == null) {

			client.createApplication(deploymentName,
					new Staging(null, localApp.getBuildpack()),
					localApp.getMemory(), localApp.getUrls(),
					localApp.getServices());
		} else if (app.getState() != AppState.STOPPED) {
			client.stopApplication(deploymentName);
		}

		client.uploadApplication(deploymentName, localApp.getLocation());
		client.startApplication(deploymentName);
	}
}
