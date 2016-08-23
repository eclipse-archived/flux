package org.eclipse.flux.core.handlers;

import java.util.Collection;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.ILiveEditConnector;
import org.json.JSONObject;

public class LiveResourceStartedHandler extends AbstractMsgHandler {
    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceStartedHandler(Collection<ILiveEditConnector> liveEditConnectors) {
        super(null, LIVE_RESOURCE_STARTED);
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String requestSenderID = message.getString(MessageConstants.REQUEST_SENDER_ID);
        int callbackID = message.getInt(MessageConstants.CALLBACK_ID);
        String username = message.getString(MessageConstants.USERNAME);
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        String hash = message.getString(MessageConstants.HASH);
        long timestamp = message.getLong(MessageConstants.TIMESTAMP);
        String liveEditID = projectName + "/" + resourcePath;
        
        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditingStarted(requestSenderID, callbackID, username, liveEditID, hash, timestamp);
        }        
    }
}
