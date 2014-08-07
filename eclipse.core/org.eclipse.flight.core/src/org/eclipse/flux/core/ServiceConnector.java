package org.eclipse.flux.core;

import org.eclipse.flux.core.internal.messaging.SocketIOMessagingConnector;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class ServiceConnector implements IServiceConnector {

	private static final String SUPER_USER = "$super$";
	
	private String username = null;

	public ServiceConnector(final String serviceID) {
		
		final IMessagingConnector messagingConnector = new SocketIOMessagingConnector(
				SUPER_USER, null);
		
		messagingConnector.addMessageHandler(new AbstractMessageHandler(
				"shutdownService") {

			@Override
			public void handleMessage(String messageType, JSONObject message) {
				try {
					if (message.getString("service").equals(serviceID)
							&& (username == null || username.equals(message.getString("username")))) {
						stopService();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		});

		messagingConnector.addMessageHandler(new AbstractMessageHandler("startServiceRequest") {

			@Override
			public void handleMessage(String messageType, JSONObject message) {
				try {
					if (serviceID.equals(message.getString("service"))) {
						String username = message.getString("username");
						String token = message.has("token") ? message.getString("token") : null;
						startService(username, token);
						ServiceConnector.this.username = username;
						JSONObject response = new JSONObject();
						response.put("service", serviceID);
						response.put("username", username);
						response.put("requestSenderID", message.getString("requestSenderID"));
						messagingConnector.send("startServiceResponse", response);
						messagingConnector.removeMessageHandler(this);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

		});
		
		messagingConnector.addConnectionListener(new IConnectionListener() {
			
			@Override
			public void disconnected() {
				// nothing
			}
			
			@Override
			public void connected() {
				try {
					JSONObject readyMessage = new JSONObject();
					readyMessage.put("service", serviceID);
					messagingConnector.send("serviceReady", readyMessage);
					messagingConnector.removeConnectionListener(this);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
		});
	}

}
