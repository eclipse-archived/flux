package org.eclipse.flux.core.handlers;

import java.util.Collection;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.ILiveEditConnector;
import org.json.JSONObject;

public class LiveResourceRequestHandler extends AbstractMsgHandler {
    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceRequestHandler(Collection<ILiveEditConnector> liveEditConnectors) {
        super(null, GET_LIVE_RESOURCE_REQUEST);
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String username = message.optString(MessageConstants.USERNAME, null);
        String requestSenderID = message.getString(MessageConstants.REQUEST_SENDER_ID);
        String projectRegEx = message.optString(MessageConstants.PROJECT_REG_EX, null);
        String resourceRegEx = message.optString(MessageConstants.RESOURCE_REG_EX, null);
        int callbackID = message.getInt(MessageConstants.CALLBACK_ID);

        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditors(requestSenderID, callbackID, username, projectRegEx, resourceRegEx);
        }        
    }

}