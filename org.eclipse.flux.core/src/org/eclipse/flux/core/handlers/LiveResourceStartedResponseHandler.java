package org.eclipse.flux.core.handlers;

import java.util.Collection;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.ILiveEditConnector;
import org.json.JSONObject;

public class LiveResourceStartedResponseHandler extends AbstractMsgHandler {
    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceStartedResponseHandler(Collection<ILiveEditConnector> liveEditConnectors) {
        super(null, LIVE_RESOURCE_STARTED_RESPONSE);
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String requestSenderID = message.getString(MessageConstants.REQUEST_SENDER_ID);
        int callbackID = message.getInt(MessageConstants.CALLBACK_ID);
        String username = message.getString(MessageConstants.USERNAME);
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        String savePointHash = message.getString(MessageConstants.SAVE_POINT_HASH);
        long savePointTimestamp = message.getLong(MessageConstants.SAVE_POINT_TIMESTAMP);
        String liveContent = message.getString(MessageConstants.LIVE_CONTENT);
        
        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditingStartedResponse(requestSenderID, callbackID, username, projectName, resourcePath, savePointHash, savePointTimestamp, liveContent);
        }        
    }
}