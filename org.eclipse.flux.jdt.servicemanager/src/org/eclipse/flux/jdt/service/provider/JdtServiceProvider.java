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
package org.eclipse.flux.jdt.service.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.service.common.CloudFoundryServiceLauncher;
import org.eclipse.flux.service.common.IServiceLauncher;
import org.eclipse.flux.service.common.LocalProcessServiceLauncher;
import org.eclipse.flux.service.common.ToolingServiceProvider;
import org.eclipse.flux.service.common.Utils;

/**
 * Starts and Stops JDT Flux service per user. JDT Tooling resources are considered to be all *.java and *.class resources.
 * 
 * @author aboyko
 *
 */
public class JdtServiceProvider {
	
	private static final String JDT_SERVICE_ID = "org.eclipse.flux.jdt";
	
	private static final String DEFAULT_FLUX_URL = "http://localhost:3000";
	
	private static final int MAX_INSTANCE_NUMBER = 100;
	
	private static final int POOL_SIZE = 3;

	private static FluxClient fluxClient = FluxClient.DEFAULT_INSTANCE;

	/**
	 * Launches the application. If command line arguments are present, the
	 * first one is considered to be the Flux server URL.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		
		URL host = null;
		if (System.getenv("FLUX_HOST") != null) {
			try {
				host = new URL(System.getenv("FLUX_HOST"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		String serviceFolderPath = null;
		
		URL cfUrl = null;
		if (System.getenv("FLUX_CF_CONTROLLER_URL") != null) {
			try {
				cfUrl = new URL(System.getenv("FLUX_CF_CONTROLLER_URL"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		String orgName = System.getenv("FLUX_CF_ORG");
		String spaceName = System.getenv("FLUX_CF_SPACE");
		String username = System.getenv("FLUX_ADMIN_ID");
		String password = System.getenv("FLUX_ADMIN_TOKEN") == null ? "" : System.getenv("FLUX_ADMIN_TOKEN");
		String cfUsername = System.getenv("FLUX_CF_USER_ID");
		String cfPassword = System.getenv("FLUX_CF_PASSWORD") == null ? "" : System.getenv("FLUX_CF_PASSWORD");
		String appId = System.getenv("FLUX_SERVICE_APP_ID") == null ? JDT_SERVICE_ID : System.getenv("FLUX_SERVICE_APP_ID");
		
		int maxInstanceNumber = MAX_INSTANCE_NUMBER;
		if (System.getenv("FLUX_SERVICE_MAX_INSTANCES") != null) {
			try {
				int n = Integer.valueOf(System.getenv("FLUX_SERVICE_MAX_INSTANCES"));
				if (n > 0) {
					maxInstanceNumber = n;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		
		int poolSize = POOL_SIZE;
		if (System.getenv("FLUX_SERVICE_POOL_SIZE") != null) {
			try {
				int n = Integer.valueOf(System.getenv("FLUX_SERVICE_POOL_SIZE"));
				if (n > 0) {
					poolSize = n;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

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
			} else if ("-appID".equals(args[i])) {
				validateArgument(args, i);
				appId = args[i+1];
			} else if ("-maxInstances".equals(args[i])) {
				validateArgument(args, i);
				int n = Integer.valueOf(args[i+1]);
				if (n > 0) {
					maxInstanceNumber = n;
				} else {
					throw new IllegalArgumentException("Max number of instances must be greater than 0");
				}
			} else if ("-poolSize".equals(args[i])) {
				validateArgument(args, i);
				int n = Integer.valueOf(args[i+1]);
				if (n > 0) {
					poolSize = n;
				} else {
					throw new IllegalArgumentException("Service pool size must be greater than 0");
				}
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
			if (cfUrl == null) {
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
			} else {
				File appFile = new File(System.getProperty("user.dir"), JDT_SERVICE_ID + ".jar");
				if (appFile.exists()) {
					serviceFolderPath = appFile.getPath();
				} else {
					throw new IllegalArgumentException("Cloud Foundry deployable service does not exist at location: " + appFile.getPath());
				} 
			}
		}
		
		boolean localDeployment = cfUrl == null;
		
		final MessageConnector messageConnector = fluxClient.connect(
				host.toString(), username, password);

		if (localDeployment) {
			serviceLauncher = createCFImmitationServiceLauncher(
					host.toString(), serviceFolderPath, username, password);
		} else {
			try {
				if (cfUsername == null) {
					throw new IllegalStateException("Cloud Foundry login credentials are not provided!");
				}
				serviceLauncher = new CloudFoundryServiceLauncher(appId, cfUrl,
						orgName, spaceName, cfUsername, cfPassword,
						host.toString(), username, password, new File(
								serviceFolderPath), maxInstanceNumber);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		final ToolingServiceProvider jdtServiceProvider = new ToolingServiceProvider(
				messageConnector, JDT_SERVICE_ID, serviceLauncher, poolSize,
				localDeployment);
		
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
		jdtServiceProvider.start();
		
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
		jdtServiceProvider.stop();
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
	
	private static LocalProcessServiceLauncher createCFImmitationServiceLauncher(String host, String serviceFolder, String login, String password) {
		List<String> command = new ArrayList<String>();
		command.add("java");
//		command.add("-Xdebug");
//		command.add("-Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y");
		command.add("-jar");
		command.add("-Dflux-host=" + host);
		command.add("-Dflux.user.name=" + login);
		command.add("-Dflux.user.token=" + password);
		command.add("-Dflux.lazyStart=true");
		command.add("-Dflux-initjdt=true");
		command.add(Utils.getEquinoxLauncherJar(serviceFolder));
		command.add("-data");
		command.add(serviceFolder + File.separator + "workspace_" + System.currentTimeMillis());
		LocalProcessServiceLauncher launcher = new LocalProcessServiceLauncher(new File(serviceFolder), command);
		return launcher;
	}
	
	private static void validateArgument(String args[], int index) {
		if (index > args.length - 2) {
			throw new RuntimeException("Argument value expected after '" + args[index] + "'");
		}
	}
}
