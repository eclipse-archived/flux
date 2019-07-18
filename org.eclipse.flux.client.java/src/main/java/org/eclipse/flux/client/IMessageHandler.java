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
    public static final String GET_PROJECT_REQUEST = "getProjectRequest";
    public static final String GET_PROJECT_RESPONSE = "getProjectResponse";
    public static final String GET_PROJECTS_REQUEST = "getProjectsRequest";
    public static final String GET_PROJECTS_RESPONSE = "getProjectsResponse";
    public static final String GET_LIVE_RESOURCE_REQUEST = "getLiveResourcesRequest";
    public static final String GET_METADATA_REQUEST = "getMetadataRequest";
    public static final String GET_METADATA_RESPONSE = "getMetadataResponse";
    public static final String GET_RESOURCE_REQUEST = "getResourceRequest";
    public static final String GET_RESOURCE_RESPONSE = "getResourceResponse";
    public static final String LIVE_RESOURCE_STARTED = "liveResourceStarted";
    public static final String LIVE_RESOURCE_STARTED_RESPONSE = "liveResourceStartedResponse";
    public static final String LIVE_RESOURCE_CHANGED = "liveResourceChanged";
    public static final String METADATA_CHANGED = "metadataChanged";
    public static final String PROJECT_CONNECTED = "projectConnected";
    public static final String PROJECT_DISCONNECTED = "projectDisconnected";
    public static final String RESOURCE_CHANGED = "resourceChanged";
    public static final String RESOURCE_CREATED = "resourceCreated";
    public static final String RESOURCE_DELETED = "resourceDeleted";
    public static final String RESOURCE_STORED = "resourceStored";
	
	boolean canHandle(String type, JSONObject message);
	
	void handle(String type, JSONObject message);
	
	String getMessageType();

}
