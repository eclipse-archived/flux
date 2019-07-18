/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flux.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.flux.client.IMessageHandler;
import org.eclipse.flux.client.MessageConnector;
import org.eclipse.flux.core.handlers.LiveResourceChangedHandler;
import org.eclipse.flux.core.handlers.LiveResourceRequestHandler;
import org.eclipse.flux.core.handlers.LiveResourceStartedHandler;
import org.eclipse.flux.core.handlers.LiveResourceStartedResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class LiveEditCoordinator {
	
    private MessageConnector messagingConnector;
	private Collection<ILiveEditConnector> liveEditConnectors;
    private Collection<IMessageHandler> messageHandlers;
	
	public LiveEditCoordinator(MessageConnector messageConnector) {
	    this.messagingConnector = messageConnector;
		this.liveEditConnectors = new CopyOnWriteArrayList<>();
	    this.messageHandlers = new ArrayList<IMessageHandler>(4);
		
		addMessageHandler(new LiveResourceStartedHandler(liveEditConnectors));
		addMessageHandler(new LiveResourceStartedResponseHandler(liveEditConnectors));
		addMessageHandler(new LiveResourceChangedHandler(liveEditConnectors));
		addMessageHandler(new LiveResourceRequestHandler(liveEditConnectors));
	}
	
	public void addLiveEditConnector(ILiveEditConnector connector) {
		liveEditConnectors.add(connector);
	}
	
	public void removeLiveEditConnector(ILiveEditConnector connector) {
		liveEditConnectors.remove(connector);
	}
	
	public void sendModelChangedMessage(String changeOriginID, String username, String projectName, String resourcePath, int offset, int removedCharactersCount, String newText) {
		try {
			JSONObject message = new JSONObject();
			message.put("username", username);
			message.put("project", projectName);
			message.put("resource", resourcePath);
			message.put("offset", offset);
			message.put("removedCharCount", removedCharactersCount);
			message.put("addedCharacters", newText != null ? newText : "");
            this.messagingConnector.send(IMessageHandler.LIVE_RESOURCE_CHANGED, message);           
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		String fullResourcePath = projectName + "/" + resourcePath;
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(changeOriginID)) {
				connector.liveEditingEvent(username, fullResourcePath, offset, removedCharactersCount, newText);
			}
		}
	}

	public void sendLiveEditStartedMessage(String changeOriginID, String username, String projectName, String resourcePath, String hash, long timestamp) {
		try {
			JSONObject message = new JSONObject();
			message.put("callback_id", 0);
			message.put("username", username);
			message.put("project", projectName);
			message.put("resource", resourcePath);
			message.put("hash", hash);
			message.put("timestamp", timestamp);
            this.messagingConnector.send(IMessageHandler.LIVE_RESOURCE_STARTED, message);			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		String path = projectName + '/' + resourcePath;
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(changeOriginID)) {
				connector.liveEditingStarted("local", 0, username, path, hash, timestamp);
			}
		}
	}
	
	public void sendLiveEditStartedResponse(String responseOriginID, String requestSenderID, int callbackID, String username, String projectName, String resourcePath, String savePointHash, long savePointTimestamp, String content) {
		try {
			JSONObject message = new JSONObject();
			message.put("requestSenderID", requestSenderID);
			message.put("callback_id", callbackID);
			message.put("username", username);
			message.put("project", projectName);
			message.put("resource", resourcePath);
			message.put("savePointTimestamp", savePointTimestamp);
			message.put("savePointHash", savePointHash);
			message.put("liveContent", content);
			this.messagingConnector.send(IMessageHandler.LIVE_RESOURCE_STARTED_RESPONSE, message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		for (ILiveEditConnector connector : this.liveEditConnectors) {
			if (!connector.getConnectorID().equals(responseOriginID)) {
				connector.liveEditingStartedResponse(requestSenderID, callbackID, username, projectName, resourcePath, savePointHash, savePointTimestamp, content);
			}
		}
	}
	
	public void sendLiveResourcesResponse(String requestSenderID,
			int callbackID, String username,
			Map<String, List<ResourceData>> liveUnits) {
		// Don't send anything if there is nothing to send
		if (liveUnits.isEmpty()) {
			return;
		}
		try {
			JSONObject message = new JSONObject();
			message.put("requestSenderID", requestSenderID);
			message.put("callback_id", callbackID);
			message.put("username", username);
			JSONObject liveEditUnits = new JSONObject();
			for (Map.Entry<String, List<ResourceData>> entry : liveUnits.entrySet()) {
				liveEditUnits.put(entry.getKey(), new JSONArray(entry.getValue()));
			}
			message.put("liveEditUnits", liveEditUnits);
            this.messagingConnector.send(IMessageHandler.GET_LIVE_RESOURCE_REQUEST, message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static class ResourceData extends JSONObject {

		public ResourceData(String path, String hash, long timestamp) {
			super();
			try {
				put("resource", path);
				put("savePointHash", hash);
				put("savePointTimestamp", timestamp);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void dispose() {
        for (IMessageHandler messageHanlder : messageHandlers) {
            messagingConnector.removeMessageHandler(messageHanlder);
        }
	}
	
	private void addMessageHandler(IMessageHandler messageHandler){
        this.messagingConnector.addMessageHandler(messageHandler);
        this.messageHandlers.add(messageHandler);
    }

}
