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
package org.springframework.social.showcase.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.social.showcase.cloudfoundry.CloudFoundryManager;
import org.springframework.social.showcase.cloudfoundry.CloudFoundryManagerImpl;
import org.springframework.social.showcase.signin.SimpleSignInAdapter;

@ComponentScan(basePackages="org.springframework.social.showcase")
@EnableConfigurationProperties
@EnableAutoConfiguration
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Bean
	public SignInAdapter signInAdapter() {
		return new SimpleSignInAdapter(new HttpSessionRequestCache());
	}

	@Bean
	public CloudFoundryManager cfm() {
		return new CloudFoundryManagerImpl();
	}
	
}