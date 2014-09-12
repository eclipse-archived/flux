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
package org.eclipse.flux.client.config;

/**
 * Configuration object containing the infos needed to make a connecion to
 * flux message bus via a flux client.
 * <p>
 * Probably this should be abstract class or interface with different implementations
 * depending on client implementation. But as of now there is only one
 * implementation. We just have a concrete class.
 */
public class FluxConfig {
	
	private final String host;
	private final String user;
	private final String token;
	
	public String getHost() {
		return host;
	}

	public String getUser() {
		return user;
	}

	public String getToken() {
		return token;
	}

	private FluxConfig(String host, String login, String token) {
		this.host = host;
		this.user = login;
		this.token = token;
	}
	
	@Override
	public String toString() {
		return user + "@" + host;
	}
	
	/**
	 * Creates a default configuration with information from environment variables or
	 * system properties. If these properties are not found some default values
	 * are inserted.'
	 */
	public static FluxConfig defaultConfig() {
		return new FluxConfig(envHost(), envLogin(), envToken());
	}
	
	public static FluxConfig superConfig() {
		return new FluxConfig(envHost(), "$super$", envToken());
	}
	
	private static String envToken() {
		String token = System.getProperty("flux-token");
		if (token==null) {
			token = System.getenv("FLUX_TOKEN");
		}
		return token;
	}

	private static String envLogin() {
		String login = System.getProperty("flux-user");
		if (login==null) {
			login = System.getenv("FLUX_USER");
		}
		if (login==null) {
			login = DEFAULT_FLUX_USER;
		}
		return login;
	}

	private static final String DEFAULT_FLUX_USER = "defaultuser";
	private static final String DEFAULT_FLUX_HOST = "http://localhost:3000";
	
	private static String envHost() {
		String fluxhost = System.getProperty("flux-host");
		if (fluxhost==null) {
			fluxhost = System.getenv("FLUX_HOST");
		}
		if (fluxhost==null) {
			fluxhost = DEFAULT_FLUX_HOST;
		}
		return fluxhost;
	}

	
	
}
