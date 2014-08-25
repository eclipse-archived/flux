package org.springframework.social.showcase.flux;

/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.springframework.social.connect.support.OAuth2ConnectionFactory;

/**
 * Github ConnectionFactory implementation.
 * @author Keith Donald
 */
public class FluxConnectionFactory extends OAuth2ConnectionFactory<Flux> {

	/**
	 * Creates a factory for GitHub connections.
	 * 
	 * @param clientId client ID
	 * @param clientSecret client secret
	 */
	public FluxConnectionFactory(String clientId, String clientSecret) {
		super("flux", new FluxServiceProvider(clientId, clientSecret), new FluxAdapter());
	}

}
