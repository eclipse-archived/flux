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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract MessageHandler implementation that implements the most common pattern
 * that almost all message handlers follow.
 * 
 * @author Kris De Volder
 */
public abstract class MessageHandler implements IMessageHandler {

	private String type;

	public MessageHandler(String type) {
		this.type = type;
	}
	
	public boolean canHandle(String type, JSONObject message) {
		return type.equals(this.type);
	}

	public abstract void handle(String type, JSONObject message);
	
	public String getMessageType() {
		return type;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"(type="+type+")";
	}
	
}
