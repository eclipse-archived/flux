package org.eclipse.flux.client;

import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public abstract class CallbackIDAwareMessageHandler extends MessageHandler {
	
	private int expectedCallbackID;

	public CallbackIDAwareMessageHandler(String messageType, int callbackID) {
		super(messageType);
		this.expectedCallbackID = callbackID;
	}
	
	@Override
	public boolean canHandle(String messageType, JSONObject message) {
		return super.canHandle(messageType, message) && message.has("callback_id") && message.optInt("callback_id") == this.expectedCallbackID;
	}

}
