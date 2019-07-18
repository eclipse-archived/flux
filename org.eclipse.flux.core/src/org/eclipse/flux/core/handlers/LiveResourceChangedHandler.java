package org.eclipse.flux.core.handlers;

import java.util.Collection;

import org.eclipse.flux.client.MessageConstants;
import org.eclipse.flux.core.ILiveEditConnector;
import org.json.JSONObject;

public class LiveResourceChangedHandler extends AbstractMsgHandler {
    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceChangedHandler(Collection<ILiveEditConnector> liveEditConnectors) {
        super(null, LIVE_RESOURCE_CHANGED);
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    protected void onMessage(String type, JSONObject message) throws Exception {
        String username = message.getString(MessageConstants.USERNAME);
        String projectName = message.getString(MessageConstants.PROJECT_NAME);
        String resourcePath = message.getString(MessageConstants.RESOURCE);
        int offset = message.getInt(MessageConstants.OFFSET);
        int removedCharCount = message.getInt(MessageConstants.REMOVED_CHAR_COUNT);
        String addedChars = message.optString(MessageConstants.ADDED_CHARACTERS);
        String liveEditID = projectName + "/" + resourcePath;

        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditingEvent(username, liveEditID, offset, removedCharCount, addedChars);
        }        
    }


}
