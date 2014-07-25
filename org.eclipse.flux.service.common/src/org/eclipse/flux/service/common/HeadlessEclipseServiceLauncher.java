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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
	
	private static final String OSGI_JAR_REGEX = "org.eclipse.equinox.launcher_.*\\.jar";
	
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
		command.add(getOsgiJar());
		command.add("-Dflux-host=" + fluxUrl);
		command.add("-Dflux.user.name=" + user);
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
			File directory = new File(getServiceHomedirectory() + File.separator + "plugins");
			if (!directory.exists()) {
				throw new IllegalArgumentException("Folder \"" + directory.getAbsolutePath() + "\" does not exist");
			}
			File[] files = directory.listFiles(new FilenameFilter() {
	
				@Override
				public boolean accept(File dir, String name) {
					return Pattern.matches(OSGI_JAR_REGEX, name);
				}
				
			});
			File latest = null;
			for (File file : files) {
				if (file.isFile()) {
					if (latest == null || latest.getName().compareTo(file.getName()) < 0) {
						latest = file;
					}
				}
			}
			if (latest == null) {
				throw new IllegalArgumentException("Cannot find 'org.eclipse.equinox.launcher' plug-in in folder: " + directory.getAbsolutePath());
			} else {
				osgiJar = latest.getAbsolutePath();
			}
		}
		return osgiJar;
	}
	
}
