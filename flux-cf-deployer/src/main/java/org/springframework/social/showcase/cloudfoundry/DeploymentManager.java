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

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of Flux project deployments per flux user.
 */
public class DeploymentManager {
	
	public static DeploymentManager INSTANCE = new DeploymentManager();
	
	private Map<String, CfFluxDeployment> deployments = new HashMap<>();

	public synchronized CfFluxDeployment get(String fluxUser, String fluxProjectName) {
		return deployments.get(fluxUser+"/"+fluxProjectName);
	}

	public synchronized void put(String fluxUser, CfFluxDeployment deployment) {
		String fluxProjectName = deployment.getFluxProjectName();
		String key = fluxUser+"/"+fluxProjectName;
		CfFluxDeployment existing = deployments.get(key);
		if (existing!=null) {
			throw new IllegalStateException("Deployment '"+key+"' already exists.");
		}
		deployments.put(key, deployment);
	}

}
