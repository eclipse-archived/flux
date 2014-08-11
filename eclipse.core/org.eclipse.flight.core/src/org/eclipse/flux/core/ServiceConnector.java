package org.eclipse.flux.core;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ServiceConnector implements IServiceConnector {

	public ServiceConnector(final IMessagingConnector serviceConnector, final String serviceID) {
		
		serviceConnector.addMessageHandler(new AbstractMessageHandler(
				"shutdownService") {

			@Override
			public void handleMessage(String messageType, JSONObject message) {
				try {
					if (message.getString("service").equals(serviceID)) {
						stopService();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		});

		serviceConnector.addMessageHandler(new AbstractMessageHandler("startServiceRequest") {

			@Override
			public void handleMessage(String messageType, JSONObject message) {
				try {
					if (serviceID.equals(message.getString("service"))) {
						serviceConnector.removeMessageHandler(this);
						String username = message.getString("username");
						startService(username);
						JSONObject response = new JSONObject();
						response.put("service", serviceID);
						response.put("username", username);
						response.put("requestSenderID", message.getString("requestSenderID"));
						serviceConnector.send("startServiceResponse", response);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

		});
		
		/*
		 * Send service ready message
		 */
		if (serviceConnector.isConnected()) {
			sendReadyMessage(serviceConnector, serviceID);
		} else {
			serviceConnector.addConnectionListener(new IConnectionListener() {

				@Override
				public void connected() {
					serviceConnector.removeConnectionListener(this);
					sendReadyMessage(serviceConnector, serviceID);
				}

				@Override
				public void disconnected() {
					// nothing
				}
				
			});
		}
	}
	
	private static void sendReadyMessage(IMessagingConnector connecgtor, String serviceID) {
		try {
			JSONObject readyMessage = new JSONObject();
			readyMessage.put("service", serviceID);
			connecgtor.send("serviceReady", readyMessage);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
