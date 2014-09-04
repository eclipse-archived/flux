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
package org.eclipse.flux.cloudfoundry.deployment.service;

import static org.eclipse.flux.client.MessageConstants.CF_CONTROLLER_URL;
import static org.eclipse.flux.client.MessageConstants.CF_LOGIN_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_PASSWORD;
import static org.eclipse.flux.client.MessageConstants.CF_PUSH_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_SPACE;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_USERNAME;
import static org.eclipse.flux.client.MessageConstants.OK;
import static org.eclipse.flux.client.MessageConstants.PROJECT_NAME;
import static org.eclipse.flux.client.MessageConstants.USERNAME;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.RequestResponseHandler;
import org.eclipse.flux.client.config.FluxConfig;
import org.json.JSONObject;

public class CfDeploymentService {
	
	private FluxClient fluxClient = FluxClient.DEFAULT_INSTANCE;
	private FluxConfig fluxConf;
	private MessageConnector flux;
	
	public CfDeploymentService(FluxClient client, FluxConfig fc) {
		this.fluxClient = client;
		this.fluxConf = fc;
	}

	/**
	 * CF clients instances for flux users that are logged in to CF.
	 */
	private Map<String, CloudFoundryClientDelegate> cfClients = Collections.synchronizedMap(
				new HashMap<String, CloudFoundryClientDelegate>());
	
	
	private synchronized CloudFoundryClientDelegate getCfClient(String username,
			String space) {
		CloudFoundryClientDelegate client = cfClients.get(username);
		if (client!=null) {
			String currentSpace = client.getSpace();
		}
		return client;
	}
	
	
	public void start() {
		this.flux = fluxClient.connect(fluxConf);
		flux.connectToChannel(fluxConf.getUser());

		flux.addMessageHandler(new RequestResponseHandler(flux, CF_LOGIN_REQUEST) {
			@Override
			public JSONObject fillResponse(String type, JSONObject req, JSONObject res) throws Exception {
				//TODO: its really not a great idea to send passwords around like that. Need to find a better solution
				//  ideally clients should get an oauth token somehow and send us that instead. 
				URL cloudControllerUrl = new URI(req.getString(CF_CONTROLLER_URL)).toURL();
				String user = req.getString(USERNAME);
				String cfUser = req.getString(CF_USERNAME);
				String password = req.getString(CF_PASSWORD);
				System.out.println("user="+user);

				CloudFoundryClientDelegate client = new CloudFoundryClientDelegate(cfUser, password, cloudControllerUrl, null);
				cfClients.put(user, client);
				res.put(OK, true);
				return res;
			}
		});
		
		flux.addMessageHandler(new RequestResponseHandler(flux, CF_SPACES_REQUEST) {
			@Override
			protected JSONObject fillResponse(String type, JSONObject req,
					JSONObject res) throws Exception {
				String user = req.getString(USERNAME);
				CloudFoundryClientDelegate cf = cfClients.get(user);
				if (cf==null) {
					return errorResponse(req, new IllegalStateException("User '"+user+"' not logged into Cloud Foundry"));
				}
				res.put(CF_SPACES, cf.getSpaces());
				return res;
			}
		});
		
		flux.addMessageHandler(new RequestResponseHandler(flux, CF_PUSH_REQUEST) {
			protected JSONObject fillResponse(String type, JSONObject req,
					JSONObject res) throws Exception {
				final String username = req.getString(USERNAME);
				final String projectName = req.getString(PROJECT_NAME);
				final String space = req.getString(CF_SPACE);
				DownloadProject downloader = new DownloadProject(flux, projectName, username);
				downloader.run(new DownloadProject.CompletionCallback() {
					@Override
					public void downloadFailed() {
						System.err.println("download project failed");
					}
					@Override
					public void downloadComplete(File project) {
						try {
							System.out.println("Downloaded project: "+project);
							
							System.out.println("Should deploy now but don't know yet how");
							CloudFoundryClientDelegate cfClient = getCfClient(username, space);
							cfClient.push(projectName, project);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				return res;
			}
		});
	}

	/**
	 * Nobody calls this right now... but anyhow
	 */
	public void shutdown() {
		MessageConnector flux = this.flux;
		if (flux!=null) {
			this.flux = null;
			flux.disconnect();
		}
	}
	
}
