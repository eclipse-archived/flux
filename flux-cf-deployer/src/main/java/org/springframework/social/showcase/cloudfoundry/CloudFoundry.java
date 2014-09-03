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

import static org.eclipse.flux.client.MessageConstants.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.client.MessageConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.social.showcase.flux.support.SingleResponseHandler;

public class CloudFoundry {

	private URL cloudControllerUrl;
	private MessageConnector flux;
	private String user;
	private boolean loggedIn = false;
	
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
			SingleResponseHandler<Void> response = new SingleResponseHandler<Void>(flux, CF_LOGIN_RESPONSE, flux.getUser()) {
				@Override
				protected Void parse(JSONObject message) throws Exception {
					boolean ok = message.getBoolean(OK);
					if (!ok) {
						throw new IOException("Login failed for unkownn reason");
					}
					return null;
				}
			};
			flux.send(CF_LOGIN_REQUEST, msg);
			
			response.awaitResult();
			loggedIn = true;
			this.user = login;
			return loggedIn;
		} catch (Throwable e) {
			e.printStackTrace();
			logout();
			return false;
		}
	}

	private void logout() {
		this.loggedIn = false;
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
			if (!loggedIn) {
				throw new IllegalStateException("Not logged in to CF");
			}
			JSONObject msg = new JSONObject()
				.put(USERNAME, flux.getUser());
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

	/**
	 * Send out a message on flux bus alerting interested parties that a project deployment config has changed.
	 */
	public void deploymentChanged(DeploymentConfig config) {
		try {
			flux.send(MessageConstants.CF_DEPLOYMENT_CHANGED,  new JSONObject()
				.put(USERNAME, flux.getUser())
				.put(CF_SPACE, config.getCfSpace())
				.put(PROJECT_NAME, config.getFluxProjectName())
				.put(ACTIVATED, config.getActivated())
			);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
