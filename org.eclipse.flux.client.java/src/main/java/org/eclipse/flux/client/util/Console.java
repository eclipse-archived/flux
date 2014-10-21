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
package org.eclipse.flux.client.util;


/**
 * Implements some of the API similar javascript console. Should make it 
 * easier to port some js code that has calls to "console.log" etc. 
 * 
 * @author kdvolder
 */
public class Console {
	
	//private String scope;
	
	private Console(String scope) {
	}
	
	public static Console get(String scope) {
		return new Console(scope);
	}

	public void log(String string) {
		System.out.println(string);
	}

	public void log(Throwable e) {
		e.printStackTrace(System.out);
	}

}
