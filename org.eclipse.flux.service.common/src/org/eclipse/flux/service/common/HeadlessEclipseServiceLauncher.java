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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Starts the headless eclipse form the command line and terminates the eclipse
 * process.
 * <p>
 * Workspace per every eclipse process started per user would the
 * <b>workspaceDirectory/user</b> folder
 * <p>
 * Folder for headless eclipse is supposed to be passed in the constructor
 * </p>
 * 
 * @author aboyko
 *
 */
public class HeadlessEclipseServiceLauncher extends CommandLineServiceLauncher {
	
	private String fluxUrl;
	
	private String workspaceDirectory;
	
	private String[] options;
	
	private String osgiJar = null;
	
	public HeadlessEclipseServiceLauncher(String eclipseDirectory, String fluxUrl, String workspaceDirectory, String[] options) {
		super(eclipseDirectory);
		this.fluxUrl = fluxUrl;
		this.workspaceDirectory = workspaceDirectory;
		this.options = options;
	}

	protected List<String> getCommand(String user) {
		List<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-jar");
		command.add("-Dflux-host=" + fluxUrl);
		command.add("-Dflux.user.name=" + user);
		command.add(getOsgiJar());
		if (options != null && options.length > 0) {
			command.addAll(Arrays.asList(options));
		}
		command.add("-data");
		if (workspaceDirectory.endsWith(File.separator)) {
			command.add(workspaceDirectory + user);
		} else {
			command.add(workspaceDirectory + File.separator + user);			
		}
		return command;
	}
	
	private String getOsgiJar() {
		if (osgiJar == null) {
			osgiJar = Utils.getEquinoxLauncherJar(getServiceHomedirectory());
		}
		return osgiJar;
	}
	
}
