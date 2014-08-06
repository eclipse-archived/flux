package org.eclipse.flux.service.common;

import org.json.JSONObject;

public interface IMessageHandler {
	
	boolean canHandle(String type, JSONObject message);
	
	void handle(String type, JSONObject message);
	
	String getMessageType();

}
