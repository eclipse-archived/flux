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

	public static final String CF_CONTROLLER_URL = "cf_controller"; // property
	public static final String CF_USERNAME = "cf_username"; // property
	public static final String CF_TOKEN = "cf_token"; //property oath2 bearer token

	public static final String OK = "ok";

	public static final String CF_SPACES_REQUEST = "cfSpacesRequest"; // message
																		// type
	public static final String CF_SPACES_RESPONSE = "cfSpacesResponse"; // message
																		// type
	public static final String CF_SPACES = "cfSpaces"; // property type

	public static final String CF_PUSH_REQUEST = "cfPushRequest"; // message
																	// type
	public static final String CF_PUSH_RESPONSE = "cfPushResponse"; // message
																	// type

	public static final String PROJECT_NAME = "project";
	public static final String CF_ORG_SPACE = "orgSpace"; // property org + "/"
															// + space

	public static final String USERNAME = "username"; // property
	public static final String ERROR = "error"; // property

	public static final String REQUEST_SENDER_ID = "requestSenderID";
	public static final String RESPONSE_SENDER_ID = "responseSenderID";
	public static final String CALLBACK_ID = "callback_id";

	public static final String CF_APP_LOG = "cfAppLog"; // message type
	public static final String CF_APP = "app";
	public static final String CF_ORG = "org";
	public static final String CF_SPACE = "space";
	public static final String CF_STREAM = "stream";
	public static final String CF_MESSAGE = "msg";

	public static final String CF_STREAM_CLIENT_ERROR = "STDERROR";
	public static final String CF_STREAM_STDOUT = "STDOUT";
	public static final String CF_STREAM_STDERROR = "STDERROR";
	public static final String CF_STREAM_SERVICE_OUT = "SVCOUT";
	
	/**
	 * Name of the Flux super user.
	 */
	public static final String SUPER_USER = "$super$";
	
    public static final String CHANNEL = "channel";
    public static final String CONNECTED_TO_CHANNEL = "connectedToChannel";
    public static final String CONTENT = "content";
    public static final String DELETED = "deleted";
    public static final String FILES = "files";
    public static final String HASH = "hash";
    public static final String INCLUDE_DELETED = "includeDeleted";
    public static final String PATH = "path";
    public static final String PROJECT = "project";
    public static final String RESOURCE = "resource";
    public static final String TIMESTAMP = "timestamp";
    public static final String TYPE = "type";
    public static final String SAVE_POINT_HASH = "savePointHash";
    public static final String SAVE_POINT_TIMESTAMP = "savePointTimestamp";
    public static final String LIVE_CONTENT = "liveContent";
    public static final String OFFSET = "offset";
    public static final String REMOVED_CHAR_COUNT = "removedCharCount";
    public static final String ADDED_CHARACTERS = "addedCharacters";
    public static final String PROJECT_REG_EX = "projectRegEx";
    public static final String RESOURCE_REG_EX = "resourceRegEx";

}
