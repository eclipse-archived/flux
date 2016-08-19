package org.eclipse.flux.core.handlers;

import java.util.Collection;

import org.eclipse.flux.core.ILiveEditConnector;
import org.eclipse.flux.watcher.core.FluxMessage;
import org.eclipse.flux.watcher.core.FluxMessageHandler;
import org.eclipse.flux.watcher.core.FluxMessageType;
import org.eclipse.flux.watcher.core.FluxMessageTypes;
import org.eclipse.flux.watcher.core.Repository;
import org.json.JSONObject;

import com.google.inject.Singleton;

@Singleton
@FluxMessageTypes({FluxMessageType.LIVE_RESOURCE_STARTED_RESPONSE})
public class LiveResourceStartedResponseHandler implements FluxMessageHandler {

    private Collection<ILiveEditConnector> liveEditConnectors;

    public LiveResourceStartedResponseHandler(Collection<ILiveEditConnector> liveEditConnectors) {
        this.liveEditConnectors = liveEditConnectors;
    }

    @Override
    public void onMessage(FluxMessage message, Repository repository) throws Exception {
        JSONObject content = message.getContent();
        String requestSenderID = content.getString(FluxMessage.Fields.REQUEST_SENDER_ID);
        int callbackID = content.getInt(FluxMessage.Fields.CALLBACK_ID);
        String username = content.getString(FluxMessage.Fields.USERNAME);
        String projectName = content.getString(FluxMessage.Fields.PROJECT);
        String resourcePath = content.getString(FluxMessage.Fields.RESOURCE);
        String savePointHash = content.getString(FluxMessage.Fields.SAVE_POINT_HASH);
        long savePointTimestamp = content.getLong(FluxMessage.Fields.SAVE_POINT_TIMESTAMP);
        String liveContent = content.getString(FluxMessage.Fields.LIVE_CONTENT);
        
        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditingStartedResponse(requestSenderID, callbackID, username, projectName, resourcePath, savePointHash, savePointTimestamp, liveContent);
        }
    }

}
