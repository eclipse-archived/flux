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

import static org.eclipse.flux.client.MessageConstants.*;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.flux.client.FluxClient;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageHandler;
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
	private Map<String, CloudFoundryClient> cfClients = Collections.synchronizedMap(
				new HashMap<String, CloudFoundryClient>());
	
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

				CloudFoundryClient client = new CloudFoundryClient(new CloudCredentials(cfUser, password), cloudControllerUrl);
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
				CloudFoundryClient cf = cfClients.get(user);
				if (cf==null) {
					return errorResponse(req, new IllegalStateException("User '"+user+"' not logged into Cloud Foundry"));
				}
				List<String> spaces = new ArrayList<>();
				for (CloudSpace space : cf.getSpaces()) {
					spaces.add(space.getName());
				}
				res.put(CF_SPACES, spaces);
				return res;
			}
		});
		
		flux.addMessageHandler(new MessageHandler(CF_DEPLOYMENT_CHANGED) {
			@Override
			public void handle(String type, JSONObject message) {
				try {
					boolean activated = message.getBoolean(ACTIVATED);
					if (activated) {
						System.out.println("Should deploy now but don't know yet how");
						String user = message.getString(USERNAME);
						CloudFoundryClient cfClient = cfClients.get(user);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
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
