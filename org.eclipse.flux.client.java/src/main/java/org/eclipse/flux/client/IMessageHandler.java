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
package org.eclipse.flux.client;

import org.json.JSONObject;

/**
 * Message Handler listener
 * 
 * @author aboyko
 *
 */
public interface IMessageHandler {
	
	boolean canHandle(String type, JSONObject message);
	
	void handle(String type, JSONObject message);
	
	String getMessageType();

}
