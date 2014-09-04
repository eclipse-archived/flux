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
import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;

/**
 * Defines API for Cloud Foundry operations, like pushing an application.
 */
public class CloudFoundryClientDelegate {

	private String cfUser;
	private URL cloudControllerUrl;
	private String password;
	private String orgSpace; // org + "/" + space

	private CloudFoundryClient client;
	private String[] spaces;
	
	public CloudFoundryClientDelegate(String cfUser, String password, URL cloudControllerUrl, String space) {
		this.cfUser = cfUser;
		this.password = password;
		this.cloudControllerUrl = cloudControllerUrl;
		this.client = createClient(cfUser, password, cloudControllerUrl, space);
	}

	private CloudFoundryClient createClient(String cfUser, String password,
			URL cloudControllerUrl, String orgSpace) {
		if (orgSpace!=null) {
			String[] pieces = orgSpace.split("/"); 
			String org = pieces[0];
			String space = pieces[1];
			return new CloudFoundryClient(
					new CloudCredentials(cfUser, password),
					cloudControllerUrl,
					org,
					space
			);
		} else {
			return new CloudFoundryClient(
					new CloudCredentials(cfUser, password),
					cloudControllerUrl
			);
		}
	}

	public void push(String appName, File location) throws IOException {
		CloudFoundryClient client = this.client;
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
	
	public String getSpace() {
		return orgSpace;
	}
	
	public synchronized void setSpace(String space) {
		try {
			if (equal(this.orgSpace, space) && client!=null) {
				return;
			}
			client = createClient(cfUser, password, cloudControllerUrl, space);
		} catch (Throwable e) {
			// something went wrong, if we still have a client, its pointing at the wrong space. So...
			// get rid of that client.
			client = null;
		}
	}

	private boolean equal(String s1, String s2) {
		if (s1==null) {
			return s1 == s2;
		} else {
			return s1.equals(s2);
		}
	}

	public synchronized String[] getSpaces() {
		//We cache this. Assume it really never changes (or at least very rarely).
		if (this.spaces==null) {
			this.spaces = fetchSpaces();
		}
		return this.spaces;
	}
	
	private String[] fetchSpaces() {
		List<CloudSpace> spaces = client.getSpaces();
		if (spaces!=null) {
			String[] array = new String[spaces.size()];
			for (int i = 0; i < array.length; i++) {
				CloudSpace space = spaces.get(i);
				array[i] = space.getOrganization().getName()+"/"+space.getName();
			}
			return array;
		}
		return new String[0];
	}

}
