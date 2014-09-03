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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Keep track of a set of objects associated with randomly generated token Strings.
 */
public class TokenMapper<S> {
	
	//TODO: expire tokens after some time.
	
	private SecureRandom random = new SecureRandom();

	public String genToken() {
		//code to generate secure token from:
		// http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
		String token;
		do {
			token = new BigInteger(130, random).toString(32);
		} while (map.containsKey(token));
		return token;
	}	
	
	private Map<String,S> map = new HashMap<>();
	
	public TokenMapper() {
	}
	
	/**
	 * Add an object. 
	 * @return new unique token that can later be used to retrieve the object
	 */
	public synchronized String put(S obj) {
		String tok = genToken();
		map.put(tok, obj);
		return tok;
	}
	
	/**
	 * Remove object
	 */
	public synchronized void remove(String token) {
		map.remove(token);
	}
	
	/**
	 * Retrieve object. Returns null if token is removed or fake.
	 */
	public synchronized S get(String token) {
		return map.get(token);
	}

}
