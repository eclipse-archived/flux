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

public class DeploymentConfig {

	private String fluxProjectName;
	private String cfSpace = null;
	private boolean activated = false;
	
	public DeploymentConfig(String fluxProjectName) {
		this.fluxProjectName = fluxProjectName;
	}
		
	public boolean getActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public String getFluxProjectName() {
		return fluxProjectName;
	}

	public String getCfSpace() {
		return cfSpace;
	}

	public void setCfSpace(String cfSpace) {
		this.cfSpace = cfSpace;
	}
	
	@Override
	public String toString() {
		return "[Deploy fluxProject '"+fluxProjectName+" to space '"+cfSpace+"']";
	}
	
}
