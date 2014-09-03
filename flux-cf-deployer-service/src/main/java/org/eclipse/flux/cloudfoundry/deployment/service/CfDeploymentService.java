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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.RequestResponseHandler;
import org.eclipse.flux.client.config.FluxConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.eclipse.flux.client.MessageConstants.*;

public class CfDeploymentService {
	
	private FluxClient fluxClient = FluxClient.DEFAULT_INSTANCE;
	private FluxConfig fluxConf;
	private MessageConnector flux;
	
	public CfDeploymentService(FluxClient client, FluxConfig fc) {
		this.fluxClient = client;
		this.fluxConf = fc;
	}
	
	private TokenMapper<CloudFoundryClient> loggedInSessions = new TokenMapper<>();
	
	public void start() {
		this.flux = fluxClient.connect(fluxConf);
		flux.connectToChannel(fluxConf.getUser());

		// LOGIN handler
		flux.addMessageHandler(new RequestResponseHandler(flux, CF_LOGIN_REQUEST) {
			@Override
			public JSONObject fillResponse(String type, JSONObject req, JSONObject res) throws Exception {
				//TODO: its really not a great idea to send passwords around like that. Need to find a better solution
				//  ideally clients should get an oauth token somehow and send us that instead. 
				URL cloudControllerUrl = new URI(req.getString(CF_CONTROLLER_URL)).toURL();
				String user = req.getString(CF_USERNAME);
				String password = req.getString(CF_PASSWORD);
				System.out.println("user="+user);

				CloudFoundryClient client = new CloudFoundryClient(new CloudCredentials(user, password), cloudControllerUrl);
				String token = loggedInSessions.put(client);
				res.put(CF_TOKEN, token);
				return res;
			}
		});
		
		flux.addMessageHandler(new RequestResponseHandler(flux, CF_SPACES_REQUEST) {
			@Override
			protected JSONObject fillResponse(String type, JSONObject req,
					JSONObject res) throws Exception {
				String token = req.getString(CF_TOKEN);
				CloudFoundryClient cf = loggedInSessions.get(token);
				List<String> spaces = new ArrayList<>();
				for (CloudSpace space : cf.getSpaces()) {
					spaces.add(space.getName());
				}
				res.put(CF_SPACES, spaces);
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
