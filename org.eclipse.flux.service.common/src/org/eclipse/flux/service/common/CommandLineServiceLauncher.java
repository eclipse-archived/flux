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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the service launcher that creates a process with a
 * shell/terminal command. The service is terminated with the process
 * termination
 * 
 * @author aboyko
 *
 */
public abstract class CommandLineServiceLauncher implements IServiceLauncher {

	private Map<String, Process> servicesHandles = new ConcurrentHashMap<String, Process>();
	
	private String serviceHomeDirectory;
	
	public CommandLineServiceLauncher(String serviceHomeDirectory) {
		super();
		if (serviceHomeDirectory.endsWith(File.pathSeparator)) {
			this.serviceHomeDirectory = serviceHomeDirectory.substring(0, serviceHomeDirectory.lastIndexOf(File.pathSeparator));
		} else {
			this.serviceHomeDirectory = serviceHomeDirectory;
		}
	}
	
	@Override
	public boolean startService(String user) {
		if (servicesHandles.containsKey(user)) {
			return false;
		}
		File logFolder = new File(this.serviceHomeDirectory + File.separator + "logs");
		if (!logFolder.exists()) {
			logFolder.mkdir();
		}
		File outputLog = new File(logFolder.getPath() + File.separator + "output_" + user + ".log");
		File errorLog = new File(logFolder.getPath() + File.separator + "error_" + user + ".log");
		try {
			outputLog.createNewFile();
			errorLog.createNewFile();
			ProcessBuilder processBuilder = new ProcessBuilder()
					.command(getCommand(user))
					.directory(new File(serviceHomeDirectory))
					.redirectError(errorLog).redirectOutput(outputLog);
			Process process = processBuilder.start();
			servicesHandles.put(user, process);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean stopService(String user) {
		Process service = servicesHandles.remove(user);
		if (service != null) {
			service.destroy();
		}
		return true;
	}
	
	protected String getServiceHomedirectory() {
		return serviceHomeDirectory;
	}
	
	abstract protected List<String> getCommand(String user);

}
