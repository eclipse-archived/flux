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

import org.json.JSONObject;
import org.json.JSONTokener;

public class JSON {

	public static JSONObject parse(String body) throws Exception {
		JSONTokener tokener = new JSONTokener(body);
		return new JSONObject(tokener);
	}
	
	public static JSONObject parse(byte[] body) throws Exception {
		return parse(new String(body, "utf8"));
	}

}
