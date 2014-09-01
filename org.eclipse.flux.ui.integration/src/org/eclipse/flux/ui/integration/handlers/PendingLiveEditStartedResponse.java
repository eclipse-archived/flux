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
package org.eclipse.flux.ui.integration.handlers;

/**
 * @author Martin Lippert
 */
public class PendingLiveEditStartedResponse {

	private String username;
	private String projectName;
	private String resource;
	private String savePointHash;
	private long savePointTimestamp;
	private String content;

	public PendingLiveEditStartedResponse(String username, String projectName, String resource, String savePointHash, long savePointTimestamp,
			String content) {
				this.username = username;
				this.projectName = projectName;
				this.resource = resource;
				this.savePointHash = savePointHash;
				this.savePointTimestamp = savePointTimestamp;
				this.content = content;
	}

	public String getUsername() {
		return username;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getResource() {
		return resource;
	}

	public String getSavePointHash() {
		return savePointHash;
	}

	public long getSavePointTimestamp() {
		return savePointTimestamp;
	}

	public String getContent() {
		return content;
	}

}
