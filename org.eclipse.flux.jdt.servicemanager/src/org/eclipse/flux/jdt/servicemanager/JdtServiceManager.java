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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.flux.service.common.IServiceLauncher;
import org.eclipse.flux.service.common.MessageCliServiceLauncher;
import org.eclipse.flux.service.common.MessageCloudFoundryServiceLauncher;
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
	
	private static final String JDT_SERVICE_CLEANUP_THREAD_ID = "Cleanup-JDT-Services";

	private static final int CLEANUP_JDT_SERVICES_CALLBACK = JDT_SERVICE_CLEANUP_THREAD_ID.hashCode();
	
	private static final String JDT_SERVICE_ID = "org.eclipse.flux.jdt";
	
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
		String password = "";
		String cfUsername = null;
		String cfPassword = "";
		IServiceLauncher serviceLauncher = null;
		
		for (int i = 0; i < args.length; i+=2) {
			if ("-host".equals(args[i])) {
				validateArgument(args, i);
				try {
					host = new URL(args[i+1]);
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException("Invalid Flux messaging server URL", e);
				}
			} else if ("-app".equals(args[i])) {
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
			} else if ("-cfuser".equals(args[i])) {
				validateArgument(args, i);
				cfUsername = args[i+1];
			} else if ("-cfpassword".equals(args[i])) {
				validateArgument(args, i);
				cfPassword = args[i+1];
			} else {
				throw new IllegalArgumentException("Invalid argument '" + args[i] + "'");
			}
		}
		
		if (username == null) {
			throw new IllegalStateException("Login credentials are not provided.");
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
			if (cfUrl != null) {
				sb.append(File.separator);
				sb.append("flux-jdt.jar");
			}
			serviceFolderPath = sb.toString();
		}
		
		final MessageConnector messageConnector = new MessageConnector(
				host.toString(), username, password);

		if (cfUrl == null) {
			serviceLauncher = createCFImmitationServiceLauncher(
					messageConnector, serviceFolderPath, username, password);
		} else {
			try {
				if (cfUsername == null) {
					throw new IllegalStateException("Cloud Foundry login credentials are not provided!");
				}
				serviceLauncher = createCloudFoundryServiceLauncher(
						messageConnector, cfUrl, orgName, spaceName,
						cfUsername, cfPassword, host.toString(), username,
						password, serviceFolderPath);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		final ToolingServiceManager jdtServiceManager = new ToolingServiceManager(
				messageConnector, serviceLauncher)
				.cleanupCallbackId(CLEANUP_JDT_SERVICES_CALLBACK)
				.fileFilters(JDT_RESOURCE_REGEX)
				.cleanupThreadId(JDT_SERVICE_CLEANUP_THREAD_ID);
		
		
		System.out.print("\nConnecting to Flux server: " + host.toString() + " ...");
		while (!messageConnector.isConnected()) {
			try {
				System.out.print('.');
				Thread.sleep(200L);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		System.out.println();
		
		System.out.println("Starting JDT service manager...");
		jdtServiceManager.start();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Type 'stop' to stop JDT services.");
		String userInput = "";
		while (!"stop".equalsIgnoreCase(userInput)) {
			try {
				userInput = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		jdtServiceManager.stop();
		messageConnector.disconnect();
		// Workaround for a defect coming from Socket IO. SocketIO doesn't terminate all threads on disconnect.
		System.exit(0);
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
	
	private static MessageCliServiceLauncher createCFImmitationServiceLauncher(MessageConnector messageConnector, String serviceFolder, String login, String password) {
		List<String> command = new ArrayList<String>();
		command.add("java");
//		command.add("-Xdebug");
//		command.add("-Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y");
		command.add("-jar");
		command.add("-Dflux-host=" + messageConnector.getHost());
		command.add("-Dflux.user.name=" + login);
		command.add("-Dflux.user.token=" + password);
		command.add("-Dflux.jdt.lazyStart=true");
		command.add(Utils.getEquinoxLauncherJar(serviceFolder));
		command.add("-data");
		command.add(serviceFolder + File.separator + "workspace_" + System.currentTimeMillis());
		MessageCliServiceLauncher launcher = new MessageCliServiceLauncher(messageConnector, JDT_SERVICE_ID, 3, 500L, new File(serviceFolder), command);
		return launcher;
	}
	
	private static MessageCloudFoundryServiceLauncher createCloudFoundryServiceLauncher(
			MessageConnector messageConnector, URL cfControllerUrl,
			String orgName, String spaceName, String cfUsername,
			String cfPassword, String fluxUrl, String username,
			String password, String serviceFolder) throws IOException {
		return new MessageCloudFoundryServiceLauncher(messageConnector,
				cfControllerUrl, orgName, spaceName, cfUsername, cfPassword,
				fluxUrl, username, password, JDT_SERVICE_ID, 3, 500L, new File(
						serviceFolder));
	}
	
	private static void validateArgument(String args[], int index) {
		if (index > args.length - 2) {
			throw new RuntimeException("Argument value expected after '" + args[index] + "'");
		}
	}
}
