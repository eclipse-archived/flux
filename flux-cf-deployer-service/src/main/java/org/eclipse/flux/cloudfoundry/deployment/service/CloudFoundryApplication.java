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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudDomain;

/**
 * Defines an application to be pushed to a Cloud Foundry server
 *
 */
public class CloudFoundryApplication {

	private final File location;

	private final CloudFoundryClient client;
	private final String appName;
	private final List<String> urls = new ArrayList<String>();

	public static final int MEMORY = 512;

	public static final String BUILD_PACK = "https://github.com/kdvolder/cf-maven-boot-buildpack";

	public CloudFoundryApplication(String appName, File location,
			CloudFoundryClient client) {
		this.location = location;
		this.appName = appName;
		this.client = client;
	}

	public int getMemory() {
		return MEMORY;
	}

	public String getBuildpack() {
		return BUILD_PACK;
	}

	public String getName() {
		return appName;
	}

	public File getLocation() {
		return location;
	}

	public List<String> getUrls() {
		if (urls.isEmpty()) {
			String defUrl = getDefaultURL();
			if (defUrl != null) {
				urls.add(defUrl);
			}
		}
		return urls;
	}

	public List<String> getServices() {
		return Collections.emptyList();
	}

	protected String getDefaultURL() {
		if (client == null) {
			return null;
		}
		CloudDomain domain = client.getDefaultDomain();

		if (domain == null) {
			List<CloudDomain> domains = client.getDomains();
			if (domains != null && !domains.isEmpty()) {
				domain = domains.get(0);
			}
		}

		if (domain != null) {
			StringWriter writer = new StringWriter();
			writer.append(getName());
			writer.append('.');
			writer.append(domain.getName());
			return writer.toString();
		}
		return null;

	}
}
