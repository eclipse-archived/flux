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
package org.springframework.social.showcase.cloudfoundry;

import static org.eclipse.flux.client.MessageConstants.CF_CONTROLLER_URL;
import static org.eclipse.flux.client.MessageConstants.CF_LOGIN_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_LOGIN_RESPONSE;
import static org.eclipse.flux.client.MessageConstants.CF_PASSWORD;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES_REQUEST;
import static org.eclipse.flux.client.MessageConstants.CF_SPACES_RESPONSE;
import static org.eclipse.flux.client.MessageConstants.CF_TOKEN;
import static org.eclipse.flux.client.MessageConstants.CF_USERNAME;
import static org.eclipse.flux.client.MessageConstants.USERNAME;

import java.net.URI;
import java.net.URL;

import org.eclipse.flux.client.MessageConnector;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.social.showcase.flux.support.SingleResponseHandler;

public class CloudFoundry {

	private URL cloudControllerUrl;
	private MessageConnector flux;
	private String cfToken;
	private String user;
	
	private DeploymentManager deployments = DeploymentManager.INSTANCE;
	private String[] spaces;

	public CloudFoundry(MessageConnector flux, String cloudControllerUrl) throws Exception {
		this.flux = flux;
		this.cloudControllerUrl = new URI(cloudControllerUrl).toURL();
	}

	public boolean login(String login, String password) {
		try {
			JSONObject msg = new JSONObject()
				.put(USERNAME, flux.getUser())
				.put(CF_CONTROLLER_URL, cloudControllerUrl.toString())
				.put(CF_USERNAME, login)
				.put(CF_PASSWORD, password);
			SingleResponseHandler<String> response = new SingleResponseHandler<String>(flux, CF_LOGIN_RESPONSE, flux.getUser()) {
				@Override
				protected String parse(JSONObject message) throws Exception {
					return message.getString(CF_TOKEN);
				}
			};
			flux.send(CF_LOGIN_REQUEST, msg);
			
			this.cfToken = response.awaitResult();
			this.user = login;
			return cfToken!=null;
		} catch (Throwable e) {
			e.printStackTrace();
			logout();
			return false;
		}
	}

	private void logout() {
		this.cfToken = null;
		this.user = null;
	}

	public String getUser() {
		return user;
	}

	public String[] getSpaces() {
		if (this.spaces!=null) {
			return spaces;
		}
		try {
			String token = this.cfToken;
			if (token==null) {
				throw new IllegalStateException("Not logged in to CF");
			}
			JSONObject msg = new JSONObject()
				.put(USERNAME, flux.getUser())
				.put(CF_TOKEN, token);
			SingleResponseHandler<String[]> response = new SingleResponseHandler<String[]>(flux, CF_SPACES_RESPONSE, flux.getUser()) {
				@Override
				protected String[] parse(JSONObject message) throws Exception {
					JSONArray _spaces = message.getJSONArray(CF_SPACES);
					String[] spaces = new String[_spaces.length()];
					for (int i = 0; i < spaces.length; i++) {
						spaces[i] = _spaces.getString(i);
					}
					return spaces;
				}
			};
			flux.send(CF_SPACES_REQUEST, msg);
			return this.spaces=response.awaitResult();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public synchronized CfFluxDeployment getFluxDeployment(String fluxProjectName) {
		return deployments.get(user, fluxProjectName);
	}

	public synchronized DeploymentConfig createDefaultDeploymentConfig(String fluxProjectName) {
		return new DeploymentConfig(fluxProjectName);
	}

	public synchronized DeploymentConfig getDeploymentConfig(String fluxProjectName) {
		CfFluxDeployment deployment = deployments.get(flux.getUser(), fluxProjectName);
		if (deployment==null) {
			return createDefaultDeploymentConfig(fluxProjectName);
		} else {
			return deployment.getConfig();
		}
	}

	public synchronized void apply(DeploymentConfig config) {
		String fluxProjectName = config.getFluxProjectName();
		String fluxUser = flux.getUser();
		CfFluxDeployment deployment = deployments.get(fluxUser, fluxProjectName);
		if (deployment==null) {
			deployment = new CfFluxDeployment(this, fluxProjectName);
			deployments.put(fluxUser, deployment);
		}
		deployment.configure(config);
	}

}
