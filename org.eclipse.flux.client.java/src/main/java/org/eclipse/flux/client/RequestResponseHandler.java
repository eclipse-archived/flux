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

import org.eclipse.flux.client.util.Assert;
import org.eclipse.flux.client.util.Console;
import org.eclipse.flux.client.util.ExceptionUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Abstract MessageHandler that provides some help in implementing a typical
 * request -> response pattern. I.e. a handler that is accepting 'request' messages
 * and sends a single response back as a result.
 * <p>
 * Subclasses must override either createResponse or fillResponse, whichever is
 * more convenient.
 * 
 * @author Kris De Volder
 */
public abstract class RequestResponseHandler extends MessageHandler {
	
	private MessageConnector flux;
	private String responseType;
	
	private static final Console console = Console.get(RequestResponseHandler.class.getName());
	
	public RequestResponseHandler(MessageConnector flux, String type) {
		super(type);
		this.flux = flux;
		this.responseType = type.replaceAll("Request$", "Response");
		Assert.assertTrue(responseType.endsWith("Response"));
	}

	public final void handle(String type, JSONObject req) {
		try {
			JSONObject res;
			try {
				res = createResponse(type, req);
			} catch (Throwable e) {
				e.printStackTrace();
				flux.send(responseType, errorResponse(req, e));
				return;
			}
			//We only get here if no exception was caught:
			flux.send(responseType, res);
		} catch (Exception e) {
			//This happens only if we had a problem sending the response
			console.log(e);
		}
	}
	
	protected JSONObject createResponse(String type, JSONObject req) throws Exception {
		JSONObject res = copy(req);
		return fillResponse(type, req, res);
	}

	protected JSONObject copy(JSONObject req) {
		try {
			JSONObject res = new JSONObject(req);
			for (String name : JSONObject.getNames(req)) {
				res.put(name, req.get(name));
			}
			return res;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Subclasses can implement this to fill in a response for the given request object.
	 * <p>
	 * If the method throws an exception it will be caugth and turned into an error response
	 * automatically.
	 * <p>
	 * If the method returns null, the request is silently ignored.
	 * <p>
	 * If the method returns a JSONObject this is sent as the response to the flux message bus.
	 * <p>
	 * For convenience a copy of the request object is passed in as as the 'res' parameter. 
	 * Implementers can add additional properties, modify the res object at will 
	 * or create a brand new object from scratch. However, when creating object from scratch
	 * is is better to override 'createResponse' directly to avoid it creating a useless 
	 * copy of the req object.
	 */
	protected JSONObject fillResponse(String type, JSONObject req, JSONObject res) throws Exception {
		return res;
	}

	protected JSONObject errorResponse(JSONObject req, Throwable e) {
		try {
			JSONObject res = copy(req);
			res.put("error", ExceptionUtil.getMessage(e));
			res.put("errorDetails", ExceptionUtil.stackTrace(e));
			return res;
		} catch (JSONException shouldNotHappen) {
			throw new RuntimeException(shouldNotHappen);
		}
	}

}
