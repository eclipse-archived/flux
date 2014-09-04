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

public abstract class MessageConstants {

	
	public static final String CF_LOGIN_REQUEST = "cfLoginRequest"; //message type
	public static final String CF_CONTROLLER_URL = "cf_controller"; //property
	public static final String CF_USERNAME = "cf_username"; //property
	public static final String CF_PASSWORD = "cf_password"; //property
	
	public static final String CF_LOGIN_RESPONSE ="cfLoginResponse"; //message type
	public static final String OK = "ok";
	
	public static final String CF_SPACES_REQUEST = "cfSpacesRequest"; //message type
	public static final String CF_SPACES_RESPONSE = "cfSpacesResponse"; //message type
	public static final String CF_SPACES = "cfSpaces"; //property type
	
	public static final String CF_PUSH_REQUEST = "cfPushRequest"; //message type
	public static final String CF_PUSH_RESPONSE = "cfPushResponse"; //message type
	
	public static final String PROJECT_NAME = "project";
	public static final String CF_ORG_SPACE = "orgSpace"; //property org + "/" + space

	public static final String USERNAME = "username";  //property
	public static final String ERROR = "error"; //property
	
	public static final String REQUEST_SENDER_ID = "requestSenderID";
	public static final String RESPONSE_SENDER_ID = "responseSenderID";
	public static final String CALLBACK_ID = "callback_id";
		
	public static final String CF_APP_LOG = "cfAppLog"; //message type
	public static final String CF_APP = "app";
	public static final String CF_ORG = "org";
	public static final String CF_SPACE = "space";
	public static final String CF_STREAM = "stream";
	public static final String CF_MESSAGE = "msg"; 
	
}
