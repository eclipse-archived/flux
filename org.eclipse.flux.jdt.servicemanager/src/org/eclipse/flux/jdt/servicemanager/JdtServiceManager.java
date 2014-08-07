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
package org.eclipse.flux.jdt.servicemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.flux.service.common.HeadlessEclipseServiceLauncher;
import org.eclipse.flux.service.common.IServiceLauncher;
import org.eclipse.flux.service.common.MessageCliServiceLauncher;
import org.eclipse.flux.service.common.MessageConnector;
import org.eclipse.flux.service.common.ToolingServiceManager;
import org.eclipse.flux.service.common.Utils;

/**
 * Starts and Stops JDT Flux service per user. JDT Tooling resources are considered to be all *.java and *.class resources.
 * 
 * @author aboyko
 *
 */
public class JdtServiceManager {

	private static final int CLEANUP_JDT_SERVICES_CALLBACK = "Cleanup-JDT-Services".hashCode();
	
	private static final String JDT_SERVICE_ID = "JDT";
	
	private static final String JDT_RESOURCE_REGEX = ".*\\.java|.*\\.class";

	/**
	 * Launches the application. If command line arguments are present, the
	 * first one is considered to be the Flux server URL.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		
		String host = null;
		String serviceFolderPath = null;
		String cfUrl = null;
		IServiceLauncher serviceLauncher = null;
		
		for (int i = 0; i < args.length; i+=2) {
			if ("-host".equals(args[i])) {
				validateArgument(args, i);
				host = args[i+1];
			} else if ("-serviceFolder".equals(args[i])) {
				validateArgument(args, i);
				serviceFolderPath = args[i+1];
			} else if ("-cfUrl".equals(args[i])) {
				validateArgument(args, i);
				cfUrl = args[i+1];
			} else {
				throw new RuntimeException("Invalid argument '" + args[i] + "'");
			}
		}
		
		if (host == null) {
			host = "http://localhost:3000"; // default Flux server URL
		}
		
		if (serviceFolderPath == null) {
			File serviceFolder = new File(System.getProperty("user.dir"));
			StringBuilder sb = new StringBuilder(serviceFolder.getParent());
			sb.append(File.separator);
			sb.append("org.eclipse.flux.headless.product");
			sb.append(File.separator);
			sb.append("target");
			sb.append(File.separator);
			sb.append("products");
			sb.append(File.separator);
			sb.append("org.eclipse.flux.headless");
			sb.append(File.separator);
			sb.append("macosx");
			sb.append(File.separator);
			sb.append("cocoa");
			sb.append(File.separator);
			sb.append("x86_64");
			serviceFolderPath = sb.toString();
		}
		
		MessageConnector messageConnector = new MessageConnector(host);
		
		if (cfUrl == null) {
			serviceLauncher = createLocalProcessServiceLauncher(host, serviceFolderPath);
		} else {
			serviceLauncher = createCloudFoundryServiceLauncher(host, serviceFolderPath, messageConnector);
		}
		
		ToolingServiceManager jdtServiceManager = new ToolingServiceManager(
				messageConnector, serviceLauncher).cleanupCallbackId(
				CLEANUP_JDT_SERVICES_CALLBACK).fileFilters(JDT_RESOURCE_REGEX);
		
		jdtServiceManager.start();
	}

	public static void deleteFolder(File folder, boolean includeFolder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f, true);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    if (includeFolder) {
	    	folder.delete();
	    }
	}
	
	private static HeadlessEclipseServiceLauncher createLocalProcessServiceLauncher(String host, String serviceFolderPath) {
		String workspaceFolderPath = serviceFolderPath + File.separator + "workspaces";
		File workspaceFolder = new File(workspaceFolderPath);
		if (workspaceFolder.exists()) {
//			if (workspaceFolder.isDirectory()) {
//				deleteFolder(workspaceFolder, false);
//			} else {
//				workspaceFolder.delete();
//			}
		} else {
			workspaceFolder.mkdir();
		}
		
		return new HeadlessEclipseServiceLauncher(serviceFolderPath, host,
				workspaceFolderPath, null);
	}
	
	private static MessageCliServiceLauncher createCloudFoundryServiceLauncher(String host, String serviceFolder, MessageConnector messageConnector) {
		List<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-jar");
		command.add("-Dflux-host=" + host);
		command.add(Utils.getEquinoxLauncherJar(serviceFolder));
		command.add("-data");
		command.add(serviceFolder + File.separator + "workspace_" + new Random().nextInt());
		MessageCliServiceLauncher launcher = new MessageCliServiceLauncher(messageConnector, JDT_SERVICE_ID, 500L, new File(serviceFolder), command);
		launcher.setServicePoolSize(3);
		return launcher;
	}
	
	private static void validateArgument(String args[], int index) {
		if (index > args.length - 2) {
			throw new RuntimeException("Argument value expected after '" + args[index] + "'");
		}
	}
}
