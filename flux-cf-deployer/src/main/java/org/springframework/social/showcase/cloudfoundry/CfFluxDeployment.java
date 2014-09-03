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

/**
 * An instance of this class represents a deployed flux application. When the deployment is activated, the 
 * flux project in question is deployed once and then monitored for changes and redeployed automatically 
 * each time it changes.
 * 
 * TODO: get rid of this class. We really only need {@link DeploymentConfig}
 */
public class CfFluxDeployment {

	private CloudFoundry cf;
	
	private String fluxProjectName;
	private String cfSpace = null;
	private boolean activated = false;
	private boolean dirty = false;
	
	public CfFluxDeployment(CloudFoundry cf, String projectName) {
		this.cf = cf;
		this.fluxProjectName = projectName;
	}
	
	@Override
	public String toString() {
		return "CfFluxDeployment("+fluxProjectName+" to space "+cfSpace+")"; 
	}
	
	public void activate() {
		if (!activated) {
			throw new Error("Not implemented yet");
		}
	}

	public String getFluxProjectName() {
		return fluxProjectName;
	}

	public synchronized DeploymentConfig getConfig() {
		DeploymentConfig conf = new DeploymentConfig(fluxProjectName);
		conf.setCfSpace(cfSpace);
		conf.setActivated(activated);
		return conf;
	}

	public synchronized void configure(DeploymentConfig config) {
		if (!config.getFluxProjectName().equals(fluxProjectName)) {
			throw new IllegalArgumentException("Can not apply DeploymentConfig. Project names don't match");
		}
		setCfSpace(config.getCfSpace());
		setActivated(config.getActivated());
		broadcastChanges(config);
	}

	private synchronized void broadcastChanges(DeploymentConfig config) {
		if (dirty) {
			cf.deploymentChanged(config);
			dirty = false;
		}
	}

	private synchronized void setActivated(boolean activated) {
		if (this.activated==activated) {
			return;
		}
		this.dirty = true;
		this.activated = activated;
	}

	private synchronized void setCfSpace(String cfSpace) {
		if (equal(this.cfSpace, cfSpace)) {
			return;
		}
		dirty = true;
		this.cfSpace = cfSpace;
	}

	/**
	 * Null-safe version of String.equals
	 */
	private boolean equal(String s1, String s2) {
		if (s1==null) {
			return s1==s2;
		}
		if (s2==null) {
			return false;
		}
		return s1.equals(s2);
	}

}
