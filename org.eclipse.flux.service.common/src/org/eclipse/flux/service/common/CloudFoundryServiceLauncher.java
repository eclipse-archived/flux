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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.springframework.http.HttpStatus;

public class CloudFoundryServiceLauncher implements IServiceLauncher {

	private CloudFoundryClient cfClient;
	private String serviceId;
	private int numberOfInstances;
	final private int maxInstancesNumber;

	public CloudFoundryServiceLauncher(String serviceId, URL cfControllerUrl, String orgName, String spaceName, String cfLogin, String cfPassword, String fluxUrl, String username, String password, 
			File appLocation, int maxInstancesNumber) throws IOException {
		this.serviceId = serviceId;
		this.numberOfInstances = 0;
		if (maxInstancesNumber < 1) {
			throw new IllegalArgumentException("Max number of instances cannot be less than 1.");
		}
		this.maxInstancesNumber = maxInstancesNumber;
		cfClient = new CloudFoundryClient(new CloudCredentials(cfLogin, cfPassword), cfControllerUrl, orgName, spaceName);
		cfClient.login();
		try {
			CloudApplication cfApp = cfClient.getApplication(serviceId);
			if (cfApp != null) {
				cfClient.deleteApplication(serviceId);
			}
		} catch (CloudFoundryException e) {
			e.printStackTrace();
		}
		cfClient.createApplication(serviceId, new Staging(), 1024, null, null);
		cfClient.uploadApplication(serviceId, appLocation, new UploadStatusCallback() {
			
			@Override
			public boolean onProgress(String arg0) {
				System.out.println("Progress: " + arg0);
				return false;
			}
			
			@Override
			public void onProcessMatchedResources(int arg0) {
				System.out.println("Matching Resources: " + arg0);
			}
			
			@Override
			public void onMatchedFileNames(Set<String> arg0) {
				System.out.println("Matching file names: " + arg0);
			}
			
			@Override
			public void onCheckResources() {
				System.out.println("Check resources!");
			}
		});		
		cfClient.updateApplicationEnv(serviceId, createEnv(fluxUrl, username, password));
	}

	@Override
	public void init() {
		cfClient.login();
		cfClient.startApplication(serviceId);
		numberOfInstances = 1;
		
		/*
		 * HACK: wait until app instance is started. Not sure how to do that with CF client API
		 */
		boolean started = false;
		while (!started) {
			try {
				cfClient.updateApplicationInstances(serviceId, numberOfInstances);
				started = true;
			} catch (Throwable t) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void startService(int n) throws Exception {
		cfClient.login();
		boolean updated = false;
		int difference = maxInstancesNumber - n - numberOfInstances;
		if (difference < 0) {
			n -= difference;
		}
		if (n > 0) {
			while (!updated) {
				try {
					cfClient.updateApplicationInstances(serviceId,
							numberOfInstances + n);
					numberOfInstances += n;
					updated = true;
				} catch (CloudFoundryException cfe) {
					if (cfe.getStatusCode() == HttpStatus.NOT_FOUND) {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						throw cfe;
					}
				}
			}
		}
		if (difference <= 0) {
			throw new Exception("Maximum number of service is running. Cannot start anymore services!");
		}
	}

	@Override
	public void dispose() {
		cfClient.login();
		cfClient.stopApplication(serviceId);
	}

	private List<String> createEnv(String fluxUrl, String username, String password) {
		List<String> env = new ArrayList<String>(3);
		env.add("FLUX_HOST=" + fluxUrl);
		env.add("FLUX_USER_ID=" + username.replace("$", "\\$"));
		env.add("FLUX_USER_TOKEN=" + password);
		env.add("FLUX_LAZY_START=true");
		return env;
	}
	
}
