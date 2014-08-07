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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.flux.service.common.HeadlessEclipseServiceLauncher;
import org.eclipse.flux.service.common.IServiceLauncher;
import org.eclipse.flux.service.common.MessageCliServiceLauncher;
import org.eclipse.flux.service.common.MessageCloudFoundryServiceLauncher;
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
	
	private static final String DEFAULT_FLUX_URL = "http://localhost:3000";

	/**
	 * Launches the application. If command line arguments are present, the
	 * first one is considered to be the Flux server URL.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		
		URL host = null;
		String serviceFolderPath = null;
		URL cfUrl = null;
		String orgName = null;
		String spaceName = null;
		String username = null;
		String password = null;
		IServiceLauncher serviceLauncher = null;
		
		for (int i = 0; i < args.length; i+=2) {
			if ("-host".equals(args[i])) {
				validateArgument(args, i);
				try {
					host = new URL(args[i+1]);
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException("Invalid Flux messaging server URL", e);
				}
			} else if ("-appFolder".equals(args[i])) {
				validateArgument(args, i);
				serviceFolderPath = args[i+1];
			} else if ("-cfUrl".equals(args[i])) {
				validateArgument(args, i);
				try {
					cfUrl = new URL(args[i+1]);
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException("Invalid Cloud Foundry controller URL", e);
				}
			} else if ("-org".equals(args[i])) {
				validateArgument(args, i);
				orgName = args[i+1];
			} else if ("-space".equals(args[i])) {
				validateArgument(args, i);
				spaceName = args[i+1];
			} else if ("-user".equals(args[i])) {
				validateArgument(args, i);
				username = args[i+1];
			} else if ("-password".equals(args[i])) {
				validateArgument(args, i);
				password = args[i+1];
			} else {
				throw new IllegalArgumentException("Invalid argument '" + args[i] + "'");
			}
		}
		
		if (host == null) {
			try {
				host = new URL(DEFAULT_FLUX_URL); // default Flux server URL
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} 
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
		
		if (cfUrl == null) {
			serviceLauncher = createLocalProcessServiceLauncher(host, serviceFolderPath);
		} else {
			if (username == null) {
				serviceLauncher = createCFImmitationServiceLauncher(host, serviceFolderPath);
			} else {
				try {
					serviceLauncher = createCloudFoundryServiceLauncher(host, cfUrl, orgName, spaceName, username, password, serviceFolderPath);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		ToolingServiceManager jdtServiceManager = new ToolingServiceManager(
				host, serviceLauncher).cleanupCallbackId(
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
	
	private static HeadlessEclipseServiceLauncher createLocalProcessServiceLauncher(URL host, String serviceFolderPath) {
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
		
		return new HeadlessEclipseServiceLauncher(serviceFolderPath, host.toString(),
				workspaceFolderPath, null);
	}
	
	private static MessageCliServiceLauncher createCFImmitationServiceLauncher(URL host, String serviceFolder) {
		List<String> command = new ArrayList<String>();
		command.add("java");
//		command.add("-Xdebug");
//		command.add("-Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y");
		command.add("-jar");
		command.add("-Dflux-host=" + host);
		command.add(Utils.getEquinoxLauncherJar(serviceFolder));
		command.add("-data");
		command.add(serviceFolder + File.separator + "workspace_" + System.currentTimeMillis());
		MessageCliServiceLauncher launcher = new MessageCliServiceLauncher(host, JDT_SERVICE_ID, 3, 500L, new File(serviceFolder), command);
		return launcher;
	}
	
	private static MessageCloudFoundryServiceLauncher createCloudFoundryServiceLauncher(URL host, URL cfControllerUrl, String orgName, String spaceName, String username, String password, String serviceFolder) throws IOException {
		return new MessageCloudFoundryServiceLauncher(host, cfControllerUrl, orgName, spaceName, username, password, JDT_SERVICE_ID, 3, 500L, new File(serviceFolder));
	}
	
	private static void validateArgument(String args[], int index) {
		if (index > args.length - 2) {
			throw new RuntimeException("Argument value expected after '" + args[index] + "'");
		}
	}
}
