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
@FluxMessageTypes({FluxMessageType.LIVE_RESOURCE_STARTED})
public class LiveResourceStartedHandler implements FluxMessageHandler {

    private Collection<ILiveEditConnector> liveEditConnectors;
    
    public LiveResourceStartedHandler(Collection<ILiveEditConnector> liveEditConnectors){
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
        String hash = content.getString(FluxMessage.Fields.HASH);
        long timestamp = content.getLong(FluxMessage.Fields.TIMESTAMP);
        String liveEditID = projectName + "/" + resourcePath;
        
        for (ILiveEditConnector connector : liveEditConnectors) {
            connector.liveEditingStarted(requestSenderID, callbackID, username, liveEditID, hash, timestamp);
        }
        
    }

}
